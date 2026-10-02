// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.ApiFixtures
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.time.Instant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeckOperationExecutorTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val executor by lazy { DeckOperationExecutor(testDeckApi(server), db, ACCOUNT, "ana") }
    private val cardPath = "${API_PATH}boards/1/stacks/10/cards"

    @AfterEach
    fun close() = db.close()

    /** A first attempt: nothing was sent before. */
    private suspend fun DeckOperationExecutor.execute(entityId: Long, operation: QueuedOperation) =
        execute(entityId, operation, maybeSent = false)

    private fun request() = server.takeRequest().let {
        "${it.method} ${it.target} ${it.body?.utf8().orEmpty()}".trim()
    }

    @Test
    fun `creates a card with its current local values and takes the server id`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(
            listOf(
                card(
                    -1,
                    title = "Edited offline"
                ).copy(description = "Notes", dueDate = Instant.parse("2026-10-04T10:00:00Z"))
            )
        )
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val result = executor.execute(
            -1,
            QueuedOperation.CreateCard(BOARD, STACK, "Queued title", 3)
        )

        assertEquals(ExecutionResult.Done(serverId = 10), result)
        assertEquals(
            """POST $cardPath {"title":"Edited offline","order":3,"type":"plain",""" +
                """"description":"Notes","duedate":"2026-10-04T10:00:00+00:00"}""",
            request()
        )
        assertNull(db.cardDao().get(ACCOUNT, -1))
        assertEquals("Edited offline", db.cardDao().get(ACCOUNT, 10)?.title)
        assertEquals("Test", db.cardSnapshotDao().get(ACCOUNT, 10)?.title)
    }

    @Test
    fun `creates a card already gone locally from the queued values`() = runTest {
        db.seedBoard()
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val result = executor.execute(-1, QueuedOperation.CreateCard(BOARD, STACK, "Queued", 0))

        assertEquals(ExecutionResult.Done(serverId = 10), result)
        assertEquals("""POST $cardPath {"title":"Queued","order":0,"type":"plain"}""", request())
        assertNull(db.cardDao().get(ACCOUNT, 10))
    }

    @Test
    fun `a failed create keeps the local card`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(-1)))
        server.enqueue(MockResponse(503))

        val result = executor.execute(-1, QueuedOperation.CreateCard(BOARD, STACK, "Card", 0))

        assertEquals(ExecutionResult.Retry("HTTP 503"), result)
        assertEquals("Card", db.cardDao().get(ACCOUNT, -1)?.title)
    }

    @Test
    fun `an update sends what changed here and keeps the server's other fields`() = runTest {
        db.seedBoard()
        val edited = card(5, title = "Now", dirty = CardField.TITLE.bit)
        db.cardDao().upsert(listOf(edited.copy(description = "Stale", order = 3)))
        db.cardSnapshotDao().put(snapshot(5, title = "Test"))
        repeat(2) { server.enqueue(json(ApiFixtures.read("card_created.json"))) }

        val result = executor.execute(5, QueuedOperation.UpdateCard(BOARD, stackId = 99))

        assertEquals(ExecutionResult.Done(), result)
        assertEquals("GET $cardPath/5", request())
        assertEquals(
            """PUT $cardPath/5 {"title":"Now","owner":"ana","order":999,"description":"",""" +
                """"type":"plain","duedate":"2019-12-24T19:29:30+00:00","archived":false}""",
            request()
        )
    }

    @Test
    fun `an update of a card deleted here sends nothing`() = runTest {
        db.seedBoard()

        assertEquals(
            ExecutionResult.Done(),
            executor.execute(5, QueuedOperation.UpdateCard(BOARD, STACK))
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `moves, archives and unarchives`() = runTest {
        repeat(3) { server.enqueue(json(ApiFixtures.read("card_created.json"))) }

        executor.execute(
            5,
            QueuedOperation.MoveCard(BOARD, fromStackId = STACK, stackId = 11, order = 2)
        )
        executor.execute(5, QueuedOperation.ArchiveCard(BOARD, STACK, archived = true))
        executor.execute(5, QueuedOperation.ArchiveCard(BOARD, STACK, archived = false))

        assertEquals(
            """PUT ${API_PATH}boards/1/stacks/11/cards/5/reorder {"order":2,"stackId":11}""",
            request()
        )
        assertEquals("PUT $cardPath/5/archive", request())
        assertEquals("PUT $cardPath/5/unarchive", request())
    }

    @Test
    fun `deleting a card the server no longer has is done`() = runTest {
        server.enqueue(MockResponse(200))
        server.enqueue(MockResponse(404))
        server.enqueue(MockResponse(403))
        val delete = QueuedOperation.DeleteCard(BOARD, STACK)

        assertEquals(ExecutionResult.Done(), executor.execute(5, delete))
        assertEquals(ExecutionResult.Done(), executor.execute(5, delete))
        assertEquals(ExecutionResult.Failed("HTTP 403"), executor.execute(5, delete))
        assertEquals("DELETE $cardPath/5", request())
    }

    @Test
    fun `sends only the label changes against the server state`() = runTest {
        db.seedBoard()
        db.labelDao().upsert((1L..3L).map { LabelEntity(ACCOUNT, it, BOARD, "L$it", "fff") })
        db.cardDao().upsert(listOf(card(5)))
        db.cardSnapshotDao().put(snapshot(5, labelIds = listOf(1, 2)))
        repeat(2) { server.enqueue(MockResponse(200)) }

        val result = executor.execute(5, QueuedOperation.SetLabels(BOARD, STACK, listOf(2, 3)))

        assertEquals(ExecutionResult.Done(), result)
        assertEquals("""PUT $cardPath/5/assignLabel {"labelId":3}""", request())
        assertEquals("""PUT $cardPath/5/removeLabel {"labelId":1}""", request())
        assertEquals(listOf(2L, 3L), db.cardSnapshotDao().get(ACCOUNT, 5)?.labelIds?.sorted())
    }

    @Test
    fun `a retry after a partial change only sends what is missing`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(5)))
        db.cardSnapshotDao().put(snapshot(5, uids = listOf("old")))
        server.enqueue(MockResponse(200))
        server.enqueue(MockResponse(500))
        server.enqueue(MockResponse(200))
        val assign = QueuedOperation.SetAssignees(BOARD, STACK, listOf("bob"))

        val first = executor.execute(5, assign)
        val second = executor.execute(5, assign)

        assertEquals(ExecutionResult.Retry("HTTP 500"), first)
        assertEquals(ExecutionResult.Done(), second)
        assertEquals("""PUT $cardPath/5/assignUser {"userId":"bob"}""", request())
        assertEquals("""PUT $cardPath/5/unassignUser {"userId":"old"}""", request())
        assertEquals("""PUT $cardPath/5/unassignUser {"userId":"old"}""", request())
        assertEquals(listOf("bob"), db.cardSnapshotDao().get(ACCOUNT, 5)?.assigneeUids)
    }

    @Test
    fun `without a known server state every member is added`() = runTest {
        repeat(2) { server.enqueue(MockResponse(200)) }

        val result = executor.execute(5, QueuedOperation.SetLabels(BOARD, STACK, listOf(7, 8)))

        assertEquals(ExecutionResult.Done(), result)
        assertEquals("""PUT $cardPath/5/assignLabel {"labelId":7}""", request())
        assertEquals("""PUT $cardPath/5/assignLabel {"labelId":8}""", request())
    }

    @Test
    fun `a title also changed on the server is kept there and marked as a conflict`() = runTest {
        db.seedBoard()
        val edited =
            card(
                5,
                title = "Mine",
                dirty = CardField.maskOf(listOf(CardField.TITLE, CardField.DESCRIPTION))
            )
        db.cardDao().upsert(listOf(edited.copy(description = "My notes")))
        db.cardSnapshotDao().put(snapshot(5, title = "Card"))
        server.enqueue(json("""{"id":5,"title":"Theirs","stackId":10,"description":null}"""))
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val result = executor.execute(5, QueuedOperation.UpdateCard(BOARD, STACK))

        assertEquals(ExecutionResult.Done(), result)
        request()
        assertEquals(
            """PUT $cardPath/5 {"title":"Theirs","owner":"ana","order":0,""" +
                """"description":"My notes","type":"plain","archived":false}""",
            request()
        )
        assertEquals(CardField.TITLE.bit, db.cardDao().get(ACCOUNT, 5)?.conflictFields)
        assertEquals("Mine", db.cardDao().get(ACCOUNT, 5)?.title)
        assertEquals("Theirs", db.cardSnapshotDao().get(ACCOUNT, 5)?.title)
    }

    @Test
    fun `a conflict on a card never synced takes the server version as known state`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(
            listOf(
                card(
                    5,
                    title = "Mine",
                    dirty = CardField.DESCRIPTION.bit
                ).copy(description = "Mine")
            )
        )
        server.enqueue(json("""{"id":5,"title":"Card","stackId":10,"description":"Theirs"}"""))
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        executor.execute(5, QueuedOperation.UpdateCard(BOARD, STACK))

        assertEquals(CardField.DESCRIPTION.bit, db.cardDao().get(ACCOUNT, 5)?.conflictFields)
        assertEquals("Theirs", db.cardSnapshotDao().get(ACCOUNT, 5)?.description)
    }

    @Test
    fun `an update waits when the server cannot be read first`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(5)))
        server.enqueue(MockResponse(503))

        assertEquals(
            ExecutionResult.Retry("HTTP 503"),
            executor.execute(5, QueuedOperation.UpdateCard(BOARD, STACK))
        )
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `after an update the server state is what was sent, so later typing is no conflict`() =
        runTest {
            db.seedBoard()
            db.cardDao().upsert(listOf(card(5, title = "Ab", dirty = CardField.TITLE.bit)))
            db.cardSnapshotDao().put(snapshot(5, title = "A"))
            server.enqueue(json("""{"id":5,"title":"A","stackId":10}"""))
            server.enqueue(json(ApiFixtures.read("card_created.json")))

            executor.execute(5, QueuedOperation.UpdateCard(BOARD, STACK))

            assertEquals("Ab", db.cardSnapshotDao().get(ACCOUNT, 5)?.title)
            assertEquals(0, db.cardDao().get(ACCOUNT, 5)?.conflictFields)
        }

    @Test
    fun `a create that may have reached the server takes the card it created`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(-1, title = "New"), card(9, title = "New")))
        server.enqueue(
            json(
                """[{"id":10,"title":"To do","boardId":1,"cards":[
                {"id":9,"title":"New","stackId":10},{"id":12,"title":"New","stackId":10}]}]"""
            )
        )

        val result = executor.execute(
            -1,
            QueuedOperation.CreateCard(BOARD, STACK, "New", 0),
            maybeSent = true
        )

        assertEquals(ExecutionResult.Done(serverId = 12), result)
        assertEquals("GET ${API_PATH}boards/1/stacks", request())
        assertEquals(1, server.requestCount)
        assertEquals("New", db.cardDao().get(ACCOUNT, 12)?.title)
    }

    @Test
    fun `a create that may have been sent but is not on the server is sent again`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(-1, title = "New")))
        server.enqueue(json("""[{"id":10,"title":"To do","boardId":1,"cards":[]}]"""))
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        val result = executor.execute(
            -1,
            QueuedOperation.CreateCard(BOARD, STACK, "New", 0),
            maybeSent = true
        )

        assertEquals(ExecutionResult.Done(serverId = 10), result)
        request()
        assertEquals("POST $cardPath", request().substringBefore(" {"))
    }

    @Test
    fun `a create waits when the server cannot be checked first`() = runTest {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(-1, title = "New")))
        server.enqueue(MockResponse(503))

        val result = executor.execute(
            -1,
            QueuedOperation.CreateCard(BOARD, STACK, "New", 0),
            maybeSent = true
        )

        assertEquals(ExecutionResult.Retry("HTTP 503"), result)
        assertEquals(1, server.requestCount)
        assertEquals("New", db.cardDao().get(ACCOUNT, -1)?.title)
    }
}
