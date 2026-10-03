// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.STACK
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BoardDeletionTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val session = mockk<AccountSession>()
    private val apis = mockk<AccountApiProvider>()
    private val deletion = BoardDeletion(session, db, apis)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        db.cardDao().upsert(listOf(card(5)))
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apis.api() } returns testDeckApi(server)
    }

    @Test
    fun `a deleted column goes with its cards`() = runTest {
        signedIn()
        server.enqueue(json("""{"id":10,"title":"To do","boardId":1}"""))

        assertEquals(ApiResult.Success(Unit), deletion.deleteColumn(BOARD, STACK))

        val request = server.takeRequest()
        assertEquals("DELETE ${API_PATH}boards/1/stacks/10", "${request.method} ${request.target}")
        assertEquals(emptyList<Any>(), db.stackDao().forBoard(ACCOUNT, BOARD))
        assertNull(db.cardDao().get(ACCOUNT, 5))
    }

    @Test
    fun `a deleted board goes with everything in it`() = runTest {
        signedIn()
        server.enqueue(json("""{"id":1,"title":"Board"}"""))

        assertEquals(ApiResult.Success(Unit), deletion.deleteBoard(BOARD))

        assertEquals(
            "DELETE ${API_PATH}boards/1",
            server.takeRequest().let {
                "${it.method} ${it.target}"
            }
        )
        assertNull(db.boardDao().get(ACCOUNT, BOARD))
        assertNull(db.cardDao().get(ACCOUNT, 5))
    }

    @Test
    fun `nothing is deleted here when the server refuses or there is no account`() = runTest {
        signedIn()
        server.enqueue(MockResponse(403))

        assertEquals(ApiResult.HttpError(403), deletion.deleteBoard(BOARD))
        every { session.activeAccount } returns flowOf(null)
        assertEquals(ApiResult.Unauthorized, deletion.deleteColumn(BOARD, STACK))
        assertEquals("Board", db.boardDao().get(ACCOUNT, BOARD)?.title)
        assertEquals(1, server.requestCount)
    }
}
