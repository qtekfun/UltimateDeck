// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import java.time.Instant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PullSyncTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val pull = PullSync(db)
    private val api by lazy { testDeckApi(server) }

    @AfterEach
    fun close() = db.close()

    private fun boardJson(id: Long, archived: Boolean = false) =
        """{"id":$id,"title":"Board $id","color":"0082c9","archived":$archived,"owner":"ana",""" +
            """"labels":[{"id":${id * 100 + 1},"title":"Bug","color":"f00"}],""" +
            """"users":[{"uid":"bob","displayname":"Bob"}],"deletedAt":0,"lastModified":1}"""

    private fun cardJson(
        id: Long,
        title: String = "Card $id",
        stackId: Long = STACK,
        labels: String = "[]",
        users: String = "[]",
        lastModified: Long = 1_790_000_000,
        extra: String = ""
    ) = """{"id":$id,"title":"$title","stackId":$stackId,"order":0,"labels":$labels,""" +
        """"assignedUsers":$users,"lastModified":$lastModified$extra}"""

    private fun stacksJson(vararg cards: String, stackId: Long = STACK, boardId: Long = BOARD) =
        """[{"id":$stackId,"title":"To do","boardId":$boardId,"order":0,""" +
            """"cards":[${cards.joinToString(",")}]}]"""

    private suspend fun seedSynced() {
        db.seedBoard()
        db.boardDao().upsert(
            listOf(BoardEntity(ACCOUNT, BOARD, "Board", "fff", stacksEtag = "\"s0\""))
        )
    }

    @Test
    fun `a first pull fills boards, columns, cards and their server state`() = runTest {
        db.accountDao().insert(AccountEntity(ACCOUNT, "https://c.example/", "ana", "Ana"))
        server.enqueue(
            json("[${boardJson(1)},${boardJson(2, archived = true)}]", 200, "ETag", "\"b1\"")
        )
        server.enqueue(
            json(
                stacksJson(
                    cardJson(
                        5,
                        labels = """[{"id":101,"title":"Bug","color":"f00"}]""",
                        users = """[{"participant":{"uid":"carl"}}]"""
                    )
                ),
                200,
                "ETag",
                "\"s1\""
            )
        )

        val result = pull.pull(api, ACCOUNT)

        assertEquals(PullResult.Done, result)
        assertEquals(2, server.requestCount)
        assertEquals("${API_PATH}boards?details=true", server.takeRequest().target)
        assertEquals("${API_PATH}boards/1/stacks", server.takeRequest().target)
        assertEquals("\"b1\"", db.accountDao().get(ACCOUNT)?.boardsEtag)
        assertEquals("\"s1\"", db.boardDao().get(ACCOUNT, 1)?.stacksEtag)
        assertTrue(db.boardDao().get(ACCOUNT, 2)!!.archived)
        assertEquals("To do", db.stackDao().forBoard(ACCOUNT, 1).single().title)
        assertEquals("Card 5", db.cardDao().get(ACCOUNT, 5)?.title)
        assertEquals(listOf(101L), db.labelDao().labelIdsOfCard(ACCOUNT, 5))
        assertEquals(listOf("carl"), db.userDao().assigneeUidsOfCard(ACCOUNT, 5))
        assertEquals("Card 5", db.cardSnapshotDao().get(ACCOUNT, 5)?.title)
    }

    @Test
    fun `unchanged lists answer 304 and nothing is written`() = runTest {
        seedSynced()
        db.accountDao().setBoardsEtag(ACCOUNT, "\"b1\"")
        server.enqueue(MockResponse(304))
        server.enqueue(MockResponse(304))

        val result = pull.pull(api, ACCOUNT)

        assertEquals(PullResult.Done, result)
        assertEquals("\"b1\"", server.takeRequest().headers["If-None-Match"])
        assertEquals("\"s0\"", server.takeRequest().headers["If-None-Match"])
        assertEquals("Board", db.boardDao().get(ACCOUNT, BOARD)?.title)
    }

    @Test
    fun `boards gone from the server are removed unless they hold local changes`() = runTest {
        seedSynced()
        db.boardDao().upsert(
            listOf(BoardEntity(ACCOUNT, 2, "Gone", "fff"), BoardEntity(ACCOUNT, 3, "Edited", "fff"))
        )
        db.stackDao().upsert(listOf(StackEntity(ACCOUNT, 30, 3, "S", 0)))
        db.cardDao().upsert(listOf(card(-1).copy(boardId = 3, stackId = 30)))
        server.enqueue(json("[${boardJson(1)}]"))
        server.enqueue(MockResponse(304))

        pull.pull(api, ACCOUNT)

        assertNull(db.boardDao().get(ACCOUNT, 2))
        assertEquals("Edited", db.boardDao().get(ACCOUNT, 3)?.title)
        assertEquals("\"s0\"", db.boardDao().get(ACCOUNT, BOARD)?.stacksEtag)
    }

    @Test
    fun `cards and columns gone from the server are removed unless edited here`() = runTest {
        seedSynced()
        db.stackDao().upsert(listOf(StackEntity(ACCOUNT, 11, BOARD, "Old column", 1)))
        db.cardDao().upsert(
            listOf(
                card(5),
                card(6, dirty = CardField.TITLE.bit),
                card(7, dirty = CardField.TITLE.bit),
                card(8, stackId = 11),
                card(-1)
            )
        )
        server.enqueue(MockResponse(304))
        server.enqueue(json(stacksJson()))
        server.enqueue(MockResponse(404))
        server.enqueue(json(cardJson(7, title = "Card", extra = ""","archived":true""")))

        val result = pull.pull(api, ACCOUNT)

        assertEquals(PullResult.Done, result)
        assertNull(db.cardDao().get(ACCOUNT, 5))
        assertNull(db.cardDao().get(ACCOUNT, 8))
        assertNull(db.stackDao().forBoard(ACCOUNT, BOARD).find { it.id == 11L })
        assertTrue(db.cardDao().get(ACCOUNT, 6)!!.deletedOnServer)
        assertTrue(db.cardDao().get(ACCOUNT, 7)!!.archived)
        assertEquals("Card", db.cardDao().get(ACCOUNT, -1)?.title)
        server.takeRequest()
        server.takeRequest()
        assertEquals("${API_PATH}boards/1/stacks/10/cards/6", server.takeRequest().target)
    }

    @Test
    fun `cards edited on both sides keep local text and newer local fields`() = runTest {
        seedSynced()
        val edited =
            card(
                5,
                title = "Mine",
                dirty = CardField.maskOf(listOf(CardField.TITLE, CardField.DUE_DATE))
            )
        db.cardDao().upsert(
            listOf(
                edited.copy(
                    dueDate = Instant.parse("2026-12-01T00:00:00Z"),
                    localModifiedAt = Instant.ofEpochSecond(1_800_000_000)
                )
            )
        )
        db.cardSnapshotDao().put(snapshot(5, title = "Base"))
        server.enqueue(MockResponse(304))
        server.enqueue(
            json(
                stacksJson(
                    cardJson(
                        5,
                        title = "Theirs",
                        extra = ""","duedate":"2026-11-01T00:00:00+00:00""""
                    )
                )
            )
        )

        pull.pull(api, ACCOUNT)

        val card = db.cardDao().get(ACCOUNT, 5)!!
        assertEquals("Mine", card.title)
        assertEquals(Instant.parse("2026-12-01T00:00:00Z"), card.dueDate)
        assertEquals(
            setOf(CardField.TITLE, CardField.DUE_DATE),
            CardField.fromMask(card.dirtyFields)
        )
        assertEquals("Theirs", db.cardSnapshotDao().get(ACCOUNT, 5)?.title)
    }

    @Test
    fun `a synced change clears the local mark`() = runTest {
        seedSynced()
        db.cardDao().upsert(
            listOf(
                card(
                    5,
                    title = "Same",
                    dirty = CardField.TITLE.bit
                ).copy(localModifiedAt = Instant.EPOCH)
            )
        )
        server.enqueue(MockResponse(304))
        server.enqueue(json(stacksJson(cardJson(5, title = "Same"))))

        pull.pull(api, ACCOUNT)

        val card = db.cardDao().get(ACCOUNT, 5)!!
        assertEquals(0, card.dirtyFields)
        assertNull(card.localModifiedAt)
    }

    @Test
    fun `a failure stops the pull and keeps boards already pulled`() = runTest {
        seedSynced()
        db.boardDao().upsert(listOf(BoardEntity(ACCOUNT, 2, "Second", "fff")))
        server.enqueue(json("[${boardJson(1)},${boardJson(2)}]"))
        server.enqueue(json(stacksJson(cardJson(5))))
        server.enqueue(MockResponse(500))

        val result = pull.pull(api, ACCOUNT)

        assertEquals(PullResult.Failed(ApiResult.HttpError(500)), result)
        assertEquals("Card 5", db.cardDao().get(ACCOUNT, 5)?.title)
    }

    @Test
    fun `a failed card lookup leaves the board as it was`() = runTest {
        seedSynced()
        db.cardDao().upsert(listOf(card(6, dirty = CardField.TITLE.bit)))
        server.enqueue(MockResponse(304))
        server.enqueue(json(stacksJson(cardJson(5)), 200, "ETag", "\"s1\""))
        server.enqueue(MockResponse(502))

        val result = pull.pull(api, ACCOUNT)

        assertEquals(PullResult.Failed(ApiResult.HttpError(502)), result)
        assertNull(db.cardDao().get(ACCOUNT, 5))
        assertEquals("\"s0\"", db.boardDao().get(ACCOUNT, BOARD)?.stacksEtag)
    }

    @Test
    fun `a failed board list stops before any board`() = runTest {
        seedSynced()
        server.enqueue(MockResponse(401))

        assertEquals(PullResult.Failed(ApiResult.Unauthorized), pull.pull(api, ACCOUNT))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `nothing is pulled for an account that no longer exists`() = runTest {
        assertEquals(PullResult.Done, pull.pull(api, ACCOUNT))
        assertEquals(0, server.requestCount)
    }
}
