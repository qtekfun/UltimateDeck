// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiFixtures
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.STACK
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArchivedCardsRepositoryTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val apis = mockk<AccountApiProvider>()
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val session = mockk<AccountSession>()
    private val repository = ArchivedCardsRepository(apis, scheduler, session, db)
    private val card = ArchivedCard(5, 1, 10, "Old", "To do", null)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apis.api() } returns testDeckApi(server)
    }

    @Test
    fun `lists archived cards with their column, newest first, without deleted ones`() = runTest {
        signedIn()
        server.enqueue(
            json(
                """[{"id":10,"title":"To do","boardId":1,"cards":[
                {"id":5,"title":"Old","stackId":10,"lastModified":100},
                {"id":6,"title":"Gone","stackId":10,"lastModified":300,"deletedAt":300}]},
                {"id":11,"title":"Done","boardId":1,"cards":[
                {"id":7,"title":"Recent","stackId":11,"lastModified":200}]}]"""
            )
        )

        val result = repository.load(BOARD)

        assertEquals("${API_PATH}boards/1/stacks/archived", server.takeRequest().target)
        assertEquals(
            ArchivedCards(
                listOf(
                    ArchivedCard(7, 1, 11, "Recent", "Done", Instant.ofEpochSecond(200)),
                    card.copy(archivedAt = Instant.ofEpochSecond(100))
                )
            ),
            result
        )
    }

    @Test
    fun `cards archived here show up before syncing, and offline too`() = runTest {
        signedIn()
        val archivedAt = Instant.ofEpochSecond(500)
        db.cardDao().upsert(
            listOf(
                card(
                    8,
                    title = "Just archived"
                ).copy(archived = true, localModifiedAt = archivedAt),
                card(5, title = "Old").copy(archived = true),
                card(9, title = "Open")
            )
        )
        server.enqueue(
            json(
                """[{"id":10,"title":"To do","boardId":1,"cards":[{"id":5,"title":"Old","stackId":10}]}]"""
            )
        )
        server.enqueue(json("[]").newBuilder().onResponseStart(SocketEffect.CloseSocket()).build())

        val online = repository.load(BOARD)
        val offline = repository.load(BOARD)

        val pending =
            ArchivedCard(8, BOARD, STACK, "Just archived", "To do", archivedAt, local = true)
        assertEquals(listOf(pending, card), online.cards)
        assertEquals(null, online.failure)
        assertEquals(listOf(8L, 5L), offline.cards.map { it.id })
        assertEquals(true, offline.failure is ApiResult.NetworkError)
    }

    @Test
    fun `restoring a card unarchives it and syncs it back`() = runTest {
        signedIn()
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        assertEquals(ApiResult.Success(Unit), repository.unarchive(card))
        assertEquals(
            "PUT ${API_PATH}boards/1/stacks/10/cards/5/unarchive",
            server.takeRequest().let { "${it.method} ${it.target}" }
        )
        verify { scheduler.requestSync() }
    }

    @Test
    fun `a failed restore does not sync`() = runTest {
        signedIn()
        server.enqueue(MockResponse(403))

        assertEquals(ApiResult.HttpError(403), repository.unarchive(card))
        verify(exactly = 0) { scheduler.requestSync() }
    }

    @Test
    fun `without an account nothing is asked`() = runTest {
        every { session.activeAccount } returns flowOf(null)
        coEvery { apis.api() } returns null

        assertEquals(ArchivedCards(emptyList(), ApiResult.Unauthorized), repository.load(BOARD))
        assertEquals(ApiResult.Unauthorized, repository.unarchive(card))
        assertEquals(0, server.requestCount)
    }
}
