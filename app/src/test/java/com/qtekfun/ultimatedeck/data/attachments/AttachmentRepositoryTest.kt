// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.attachments

import android.net.Uri
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.STACK
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import com.qtekfun.ultimatedeck.sync.queue.FixedRandom
import com.qtekfun.ultimatedeck.sync.queue.MutableClock
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AttachmentRepositoryTest {
    @StartStop
    val server = MockWebServer()

    @TempDir
    lateinit var dir: File

    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val session = mockk<AccountSession>()
    private val apis = mockk<AccountApiProvider>()
    private val files = mockk<AttachmentFiles>()
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val repository =
        AttachmentRepository(session, db, files, apis, queue, scheduler)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(5)))
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apis.api() } returns testDeckApi(server)
        every { files.fileFor(any(), any(), any()) } answers
            { File(dir, "${secondArg<Long>()}-${thirdArg<String>()}") }
        coEvery { files.delete(any()) } answers { File(firstArg<String>()).delete() }
        coEvery { files.save(any(), any()) } answers {
            secondArg<File>().also { target ->
                target.outputStream().use { firstArg<InputStream>().copyTo(it) }
            }
        }
    }

    private fun attachment(
        id: Long,
        state: UploadState = UploadState.DONE,
        localUri: String? = null
    ) = AttachmentEntity(
        ACCOUNT,
        id,
        5,
        "f$id.txt",
        "text/plain",
        1,
        localUri = localUri,
        uploadState = state
    )

    private suspend fun stored() = db.attachmentDao().observeForCard(ACCOUNT, 5).first()

    private suspend fun queued() = db.pendingOperationDao().all(ACCOUNT).map {
        it.entityId to
            QueuedOperation.decode(it.payload)
    }

    @Test
    fun `an added file waits as pending and is queued for upload`() = runTest {
        signedIn()
        val copy = File(dir, "photo.png").apply { writeText("PNG") }
        coEvery { files.import(any(), ACCOUNT, any()) } returns
            ImportedFile(copy, "photo.png", "image/png")

        assertTrue(repository.add(5, mockk<Uri>()))

        val added = stored().single()
        assertEquals(UploadState.PENDING to "photo.png", added.uploadState to added.fileName)
        assertEquals(3L to copy.path, added.size to added.localUri)
        assertTrue(added.id < 0)
        assertEquals(
            listOf(added.id to QueuedOperation.UploadAttachment(BOARD, STACK, 5)),
            queued()
        )
        verify { scheduler.requestSync() }
    }

    @Test
    fun `a file that cannot be read adds nothing`() = runTest {
        signedIn()
        coEvery { files.import(any(), any(), any()) } returns null

        assertFalse(repository.add(5, mockk<Uri>()))
        assertFalse(repository.add(9, mockk<Uri>()))
        assertEquals(emptyList<Any>(), stored())
    }

    @Test
    fun `refreshing replaces synced attachments and keeps pending ones and cached files`() =
        runTest {
            signedIn()
            db.attachmentDao().upsert(
                listOf(
                    attachment(7, localUri = "/cached/7"),
                    attachment(8),
                    attachment(-1, UploadState.PENDING)
                )
            )
            server.enqueue(
                json(
                    """[{"id":7,"cardId":5,"type":"file","data":"f7.txt"},
                {"id":9,"cardId":5,"type":"deck_file","data":"old.txt"},
                {"id":10,"cardId":5,"type":"file","data":"gone.txt","deletedAt":5}]"""
                )
            )

            assertEquals(ApiResult.Success(Unit), repository.refresh(5))

            assertEquals(
                "${API_PATH}boards/1/stacks/10/cards/5/attachments",
                server.takeRequest().target
            )
            assertEquals(listOf(-1L, 7L, 9L), stored().map { it.id }.sorted())
            assertEquals("/cached/7", db.attachmentDao().get(ACCOUNT, 7)?.localUri)
            assertEquals("deck_file", db.attachmentDao().get(ACCOUNT, 9)?.type)
        }

    @Test
    fun `opening uses the copy on this device or downloads it once`() = runTest {
        signedIn()
        val cached = File(dir, "cached.txt").apply { writeText("here") }
        db.attachmentDao().upsert(
            listOf(attachment(7, localUri = cached.path), attachment(9).copy(type = "deck_file"))
        )
        server.enqueue(MockResponse(200, headersOf(), "downloaded"))

        assertEquals(
            ApiResult.Success(cached),
            repository.open(db.attachmentDao().get(ACCOUNT, 7)!!)
        )
        val downloaded = repository.open(db.attachmentDao().get(ACCOUNT, 9)!!) as ApiResult.Success

        assertEquals(
            "${API_PATH}boards/1/stacks/10/cards/5/attachments/deck_file/9",
            server.takeRequest().target
        )
        assertEquals("downloaded", downloaded.value.readText())
        assertEquals(downloaded.value.path, db.attachmentDao().get(ACCOUNT, 9)?.localUri)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a failed upload can be retried or discarded`() = runTest {
        signedIn()
        val copy = File(dir, "big.bin").apply { writeText("x") }
        db.attachmentDao().upsert(
            listOf(
                attachment(-1, UploadState.FAILED, copy.path),
                attachment(-2, UploadState.FAILED)
            )
        )
        queue.enqueue(ACCOUNT, -1, QueuedOperation.UploadAttachment(BOARD, STACK, 5))
        queue.enqueue(ACCOUNT, -2, QueuedOperation.UploadAttachment(BOARD, STACK, 5))
        db.pendingOperationRetryDao().markFailed(
            db.pendingOperationDao().all(ACCOUNT).last().id,
            "413"
        )

        repository.retry(db.attachmentDao().get(ACCOUNT, -2)!!)
        repository.discard(db.attachmentDao().get(ACCOUNT, -1)!!)

        assertEquals(listOf(-2L), stored().map { it.id })
        assertEquals(UploadState.PENDING, db.attachmentDao().get(ACCOUNT, -2)?.uploadState)
        assertFalse(db.pendingOperationDao().all(ACCOUNT).single().failed)
        assertFalse(copy.exists())
    }

    @Test
    fun `an uploaded attachment deleted here goes at once and is not brought back`() = runTest {
        signedIn()
        val copy = File(dir, "7.txt").apply { writeText("x") }
        db.attachmentDao().upsert(
            listOf(
                attachment(7, localUri = copy.path).copy(type = "deck_file"),
                attachment(-1, UploadState.PENDING)
            )
        )
        server.enqueue(json("""[{"id":7,"cardId":5,"type":"deck_file","data":"f7.txt"}]"""))

        repository.delete(db.attachmentDao().get(ACCOUNT, 7)!!)
        repository.delete(db.attachmentDao().get(ACCOUNT, -1)!!)
        repository.refresh(5)

        assertEquals(emptyList<Any>(), stored())
        assertFalse(copy.exists())
        assertEquals(
            listOf(7L to QueuedOperation.DeleteAttachment(BOARD, STACK, 5, "deck_file")),
            queued()
        )
        verify { scheduler.requestSync() }
    }
}
