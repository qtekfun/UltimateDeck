// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.io.File
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AttachmentUploaderTest {
    @StartStop
    val server = MockWebServer()

    @TempDir
    lateinit var dir: File

    private val db = inMemoryDatabase()
    private val executor by lazy { DeckOperationExecutor(testDeckApi(server), db, ACCOUNT, "ana") }
    private val upload = QueuedOperation.UploadAttachment(BOARD, STACK, 5)

    @AfterEach
    fun close() = db.close()

    private suspend fun attached(
        cardId: Long = 5,
        file: File? = File(dir, "photo.png").apply {
            writeText("PNG")
        }
    ) {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(cardId)))
        db.attachmentDao().upsert(
            listOf(
                AttachmentEntity(
                    accountId = ACCOUNT,
                    id = -1,
                    cardId = cardId,
                    fileName = "photo.png",
                    mimeType = "image/png",
                    size = 3,
                    localUri = file?.path,
                    uploadState = UploadState.PENDING
                )
            )
        )
    }

    private suspend fun state() = db.attachmentDao().get(ACCOUNT, -1)?.uploadState

    @Test
    fun `an upload replaces the local attachment with the server one`() = runTest {
        attached()
        server.enqueue(
            json(
                """{"id":7,"cardId":5,"type":"file","data":"photo.png","extendedData":{"filesize":3}}"""
            )
        )

        val result = executor.execute(-1, upload, maybeSent = false)

        assertEquals(ExecutionResult.Done(serverId = 7), result)
        val request = server.takeRequest()
        assertEquals(
            "POST ${API_PATH}boards/1/stacks/10/cards/5/attachments",
            "${request.method} ${request.target}"
        )
        assertTrue("filename=\"photo.png\"" in request.body!!.utf8())
        assertNull(db.attachmentDao().get(ACCOUNT, -1))
        val stored = db.attachmentDao().get(ACCOUNT, 7)!!
        assertEquals(UploadState.DONE, stored.uploadState)
        assertEquals(File(dir, "photo.png").path, stored.localUri)
    }

    @Test
    fun `a file the server refuses waits for the user`() = runTest {
        attached()
        server.enqueue(MockResponse(413))

        assertEquals(
            ExecutionResult.Failed("HTTP 413"),
            executor.execute(-1, upload, maybeSent = false)
        )
        assertEquals(UploadState.FAILED, state())
    }

    @Test
    fun `a temporary failure keeps it pending for the next try`() = runTest {
        attached()
        server.enqueue(MockResponse(503))

        assertEquals(
            ExecutionResult.Retry("HTTP 503"),
            executor.execute(-1, upload, maybeSent = false)
        )
        assertEquals(UploadState.PENDING, state())
    }

    @Test
    fun `an attachment of a card not created yet waits for it`() = runTest {
        attached(cardId = -3)

        assertEquals(
            ExecutionResult.Retry("card not created yet"),
            executor.execute(-1, upload, maybeSent = false)
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a lost local file cannot be uploaded`() = runTest {
        attached(file = File(dir, "gone.png"))

        assertEquals(
            ExecutionResult.Failed("file missing"),
            executor.execute(-1, upload, maybeSent = false)
        )
        assertEquals(UploadState.FAILED, state())
    }

    @Test
    fun `an attachment already gone needs nothing`() = runTest {
        assertEquals(ExecutionResult.Done(), executor.execute(-9, upload, maybeSent = false))
    }

    @Test
    fun `deleting an attachment on the server is done even if it is already gone`() = runTest {
        server.enqueue(MockResponse(200))
        server.enqueue(MockResponse(404))
        server.enqueue(MockResponse(500))
        val delete = QueuedOperation.DeleteAttachment(BOARD, STACK, 5, "deck_file")

        assertEquals(ExecutionResult.Done(), executor.execute(7, delete, maybeSent = false))
        assertEquals(ExecutionResult.Done(), executor.execute(7, delete, maybeSent = false))
        assertEquals(
            ExecutionResult.Retry("HTTP 500"),
            executor.execute(7, delete, maybeSent = false)
        )
        val request = server.takeRequest()
        assertEquals(
            "DELETE ${API_PATH}boards/1/stacks/10/cards/5/attachments/deck_file/7",
            "${request.method} ${request.target}"
        )
    }
}
