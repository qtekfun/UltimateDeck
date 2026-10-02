// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiFixtures
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArchivedCardsRepositoryTest {
    @StartStop
    val server = MockWebServer()

    private val apis = mockk<AccountApiProvider>()
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val repository = ArchivedCardsRepository(apis, scheduler)
    private val card = ArchivedCard(5, 1, 10, "Old", "To do", null)

    private fun signedIn() = coEvery { apis.api() } returns testDeckApi(server)

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

        val result = repository.load(1)

        assertEquals("${API_PATH}boards/1/stacks/archived", server.takeRequest().target)
        assertEquals(
            ApiResult.Success(
                listOf(
                    ArchivedCard(7, 1, 11, "Recent", "Done", Instant.ofEpochSecond(200)),
                    card.copy(archivedAt = Instant.ofEpochSecond(100))
                )
            ),
            result
        )
    }

    @Test
    fun `failures are reported as they are`() = runTest {
        signedIn()
        server.enqueue(MockResponse(500))

        assertEquals(ApiResult.HttpError(500), repository.load(1))
    }

    @Test
    fun `restoring a card unarchives it and syncs it back`() = runTest {
        signedIn()
        server.enqueue(json(ApiFixtures.read("card_created.json")))

        assertEquals(ApiResult.Success(Unit), repository.unarchive(card))
        assertEquals(
            "PUT ${API_PATH}boards/1/stacks/10/cards/5/unarchive",
            server.takeRequest().let {
                "${it.method} ${it.target}"
            }
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
        coEvery { apis.api() } returns null

        assertEquals(ApiResult.Unauthorized, repository.load(1))
        assertEquals(ApiResult.Unauthorized, repository.unarchive(card))
        assertEquals(0, server.requestCount)
    }
}
