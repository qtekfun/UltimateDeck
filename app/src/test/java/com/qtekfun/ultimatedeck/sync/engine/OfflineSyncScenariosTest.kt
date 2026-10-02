// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.domain.card.CardActions
import com.qtekfun.ultimatedeck.sync.queue.FixedRandom
import com.qtekfun.ultimatedeck.sync.queue.MutableClock
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.ProcessResult
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Whole syncs against a scripted server (T10, SPEC §7): the real engine, queue, executor and
 * pull over an in-memory database, through network failures, server errors, concurrent changes
 * and an app closed halfway.
 */
class OfflineSyncScenariosTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val clock = MutableClock()
    private val queue = OperationQueue(db, clock, FixedRandom(0.5))
    private val session = mockk<AccountSession>(relaxed = true)
    private val apiProvider = mockk<AccountApiProvider>()
    private val actions = CardActions(session, db, queue, clock, mockk(relaxed = true))
    private var seen = 0

    @AfterEach
    fun close() = db.close()

    /** A new engine is what a restarted app gets: only the database is kept. */
    private fun engine() =
        SyncEngine(session, apiProvider, db, queue, PullSync(db, queue), Dispatchers.IO)

    private suspend fun signedIn() {
        db.seedBoard()
        db.boardDao().upsert(
            listOf(BoardEntity(ACCOUNT, BOARD, "Board", "fff", stacksEtag = "\"s0\""))
        )
        every { session.credentials() } returns Credentials("ana", "secret")
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apiProvider.api() } returns testDeckApi(server)
    }

    /** A card known from an earlier sync: its row and its last server state. */
    private suspend fun synced(id: Long, title: String = "Card") {
        db.cardDao().upsert(listOf(card(id, title = title)))
        db.cardSnapshotDao().put(snapshot(id, title = title))
    }

    private fun cardJson(
        id: Long,
        title: String = "Card",
        description: String = "",
        stackId: Long = STACK
    ) = """{"id":$id,"title":"$title","description":"$description","stackId":$stackId,"order":0}"""

    private fun stacksJson(vararg cards: String, stackId: Long = STACK, boardId: Long = BOARD) =
        """[{"id":$stackId,"title":"To do","boardId":$boardId,"cards":[${cards.joinToString(
            ","
        )}]}]"""

    private fun ok(body: String = cardJson(5)) = json(body)

    private fun unchanged() = MockResponse(304)

    /** The connection hangs past the client timeout: OkHttp does not retry that by itself. */
    private fun timedOut() = reordered().newBuilder().headersDelay(2, TimeUnit.SECONDS).build()

    /** Deck answers a move with the cards of the column. */
    private fun reordered() = json("[]")

    /** Requests the server got since the last call, as "METHOD path". */
    private fun requests(): List<String> {
        val new = List(server.requestCount - seen) { server.takeRequest().describe() }
        seen = server.requestCount
        return new
    }

    private fun RecordedRequest.describe() = "$method ${target.substringAfter(API_PATH)}"

    private fun bodyOf(request: RecordedRequest) = request.body?.utf8().orEmpty()

    @Test
    fun `a connection lost while sending loses nothing and resends only what failed`() = runTest {
        signedIn()
        synced(5)
        synced(6)
        queue.enqueue(ACCOUNT, 5, QueuedOperation.ArchiveCard(BOARD, STACK, true))
        queue.enqueue(ACCOUNT, 5, QueuedOperation.MoveCard(BOARD, STACK, STACK, 2))
        queue.enqueue(ACCOUNT, 6, QueuedOperation.ArchiveCard(BOARD, STACK, true))
        listOf(ok(), timedOut(), ok(), unchanged(), unchanged()).forEach(server::enqueue)

        assertEquals(SyncOutcome.Ok(ProcessResult(done = 2, retried = 1)), engine().sync())
        assertEquals(
            listOf(
                "PUT boards/1/stacks/10/cards/5/archive",
                "PUT boards/1/stacks/10/cards/5/reorder",
                "PUT boards/1/stacks/10/cards/6/archive",
                "GET boards?details=true",
                "GET boards/1/stacks"
            ),
            requests()
        )

        // Too early: the failed move waits for its backoff.
        listOf(unchanged(), unchanged()).forEach(server::enqueue)
        engine().sync()
        assertEquals(listOf("GET boards?details=true", "GET boards/1/stacks"), requests())

        clock.advance(Duration.ofMinutes(1))
        listOf(reordered(), unchanged(), unchanged()).forEach(server::enqueue)
        engine().sync()
        assertEquals("PUT boards/1/stacks/10/cards/5/reorder", requests().first())
        assertEquals(emptyList<Any>(), db.pendingOperationDao().all(ACCOUNT))
    }

    @Test
    fun `a failure while pulling keeps finished boards and completes the rest next time`() =
        runTest {
            signedIn()
            db.boardDao().upsert(
                listOf(BoardEntity(ACCOUNT, 2, "Second", "fff", stacksEtag = "\"b2\""))
            )
            db.stackDao().upsert(listOf(StackEntity(ACCOUNT, 20, 2, "Todo", 0)))
            val boards = """[{"id":1,"title":"Board","color":"fff"},""" +
                """{"id":2,"title":"Second","color":"fff"}]"""
            server.enqueue(json(boards, 200, "ETag", "\"l1\""))
            server.enqueue(json(stacksJson(cardJson(5)), 200, "ETag", "\"e1\""))
            server.enqueue(MockResponse(500))

            assertEquals(SyncOutcome.Error("HTTP 500"), engine().sync())
            assertEquals("Card", db.cardDao().get(ACCOUNT, 5)?.title)
            assertEquals("\"b2\"", db.boardDao().get(ACCOUNT, 2)?.stacksEtag)
            requests()

            server.enqueue(unchanged())
            server.enqueue(unchanged())
            server.enqueue(json(stacksJson(cardJson(7, stackId = 20), stackId = 20, boardId = 2)))

            assertEquals(SyncOutcome.Ok(ProcessResult()), engine().sync())
            assertEquals("\"l1\"", server.takeRequest().headers["If-None-Match"])
            assertEquals("\"e1\"", server.takeRequest().headers["If-None-Match"])
            assertEquals("Card", db.cardDao().get(ACCOUNT, 7)?.title)
        }

    @Test
    fun `a refused change waits for the user without blocking other cards`() = runTest {
        signedIn()
        synced(5)
        synced(6)
        queue.enqueue(ACCOUNT, 5, QueuedOperation.ArchiveCard(BOARD, STACK, true))
        queue.enqueue(ACCOUNT, 6, QueuedOperation.ArchiveCard(BOARD, STACK, true))
        listOf(MockResponse(403), ok(), unchanged(), unchanged()).forEach(server::enqueue)

        assertEquals(SyncOutcome.Ok(ProcessResult(done = 1, failed = 1)), engine().sync())
        val refused = db.pendingOperationDao().all(ACCOUNT).single()
        assertTrue(refused.failed)
        requests()

        clock.advance(Duration.ofHours(2))
        listOf(unchanged(), unchanged()).forEach(server::enqueue)
        engine().sync()
        assertEquals(listOf("GET boards?details=true", "GET boards/1/stacks"), requests())

        queue.retry(refused.id)
        listOf(ok(), unchanged(), unchanged()).forEach(server::enqueue)
        engine().sync()
        assertEquals("PUT boards/1/stacks/10/cards/5/archive", requests().first())
        assertEquals(emptyList<Any>(), db.pendingOperationDao().all(ACCOUNT))
    }

    @Test
    fun `my title and their description both survive`() = runTest {
        signedIn()
        synced(5)
        actions.editTitle(5, "Mine")
        server.enqueue(ok(cardJson(5, description = "Theirs")))
        server.enqueue(ok())
        server.enqueue(unchanged())
        server.enqueue(json(stacksJson(cardJson(5, title = "Mine", description = "Theirs"))))

        engine().sync()

        server.takeRequest()
        val put = bodyOf(server.takeRequest())
        assertTrue(""""title":"Mine"""" in put, put)
        assertTrue(""""description":"Theirs"""" in put, put)
        val card = db.cardDao().get(ACCOUNT, 5)!!
        assertEquals("Mine" to "Theirs", card.title to card.description)
        assertEquals(0, card.dirtyFields)
    }

    @Test
    fun `the same title changed on both sides overwrites nobody`() = runTest {
        signedIn()
        synced(5)
        actions.editTitle(5, "Mine")
        server.enqueue(ok(cardJson(5, title = "Theirs")))
        server.enqueue(ok(cardJson(5, title = "Theirs")))
        server.enqueue(unchanged())
        server.enqueue(json(stacksJson(cardJson(5, title = "Theirs"))))

        engine().sync()

        server.takeRequest()
        assertTrue(""""title":"Theirs"""" in bodyOf(server.takeRequest()))
        val card = db.cardDao().get(ACCOUNT, 5)!!
        assertEquals("Mine", card.title)
        assertEquals(CardField.TITLE.bit, card.conflictFields)
    }

    @Test
    fun `a card deleted on the server while edited here is kept for the user to decide`() =
        runTest {
            signedIn()
            synced(5)
            actions.editTitle(5, "Mine")
            listOf(
                MockResponse(404),
                unchanged(),
                json(stacksJson()),
                MockResponse(404)
            ).forEach(server::enqueue)

            engine().sync()

            val card = db.cardDao().get(ACCOUNT, 5)!!
            assertTrue(card.deletedOnServer)
            assertEquals("Mine", card.title)
        }

    @Test
    fun `a create cut off by closing the app is not duplicated when the app comes back`() =
        runTest {
            signedIn()
            actions.create(BOARD, STACK, "New")
            server.enqueue(
                ok(
                    cardJson(12, title = "New")
                ).newBuilder().headersDelay(5, TimeUnit.SECONDS).build()
            )

            val closing = launch(Dispatchers.IO) { engine().sync() }
            assertEquals("POST boards/1/stacks/10/cards", server.takeRequest().describe())
            closing.cancelAndJoin()
            seen = server.requestCount

            server.enqueue(json(stacksJson(cardJson(12, title = "New"))))
            server.enqueue(unchanged())
            server.enqueue(json(stacksJson(cardJson(12, title = "New"))))
            assertEquals(SyncOutcome.Ok(ProcessResult(done = 1)), engine().sync())

            assertEquals(
                listOf("GET boards/1/stacks", "GET boards?details=true", "GET boards/1/stacks"),
                requests()
            )
            assertEquals(listOf(12L), db.cardDao().allForBoard(ACCOUNT, BOARD).map { it.id })
            assertEquals(emptyList<Any>(), db.pendingOperationDao().all(ACCOUNT))
        }

    @Test
    fun `two syncs at once send each change once`() = runTest {
        signedIn()
        synced(5)
        queue.enqueue(ACCOUNT, 5, QueuedOperation.ArchiveCard(BOARD, STACK, true))
        server.enqueue(ok().newBuilder().headersDelay(300, TimeUnit.MILLISECONDS).build())
        repeat(4) { server.enqueue(unchanged()) }

        // The app has a single engine: the worker and pull-to-refresh share it.
        val engine = engine()
        val worker = async(Dispatchers.IO) { engine.sync() }
        val refresh = async(Dispatchers.IO) { engine.sync() }
        worker.await()
        refresh.await()

        assertEquals(1, requests().count { it.startsWith("PUT") })
    }
}
