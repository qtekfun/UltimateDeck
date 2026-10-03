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
import com.qtekfun.ultimatedeck.domain.board.BoardItem
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardCreationTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val session = mockk<AccountSession>()
    private val apis = mockk<AccountApiProvider>()
    private val creation = BoardCreation(session, db, apis)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apis.api() } returns testDeckApi(server)
    }

    @Test
    fun `a new board is created on the server and kept here with its owner as member`() = runTest {
        signedIn()
        server.enqueue(
            json(
                """{"id":7,"title":"Trips","color":"00a15f","owner":{"uid":"ana","displayname":"Ana"}}"""
            )
        )

        val result = creation.createBoard("  Trips ", "00a15f")

        assertEquals(ApiResult.Success(BoardItem(7, "Trips", "00a15f")), result)
        val request = server.takeRequest()
        assertEquals("POST ${API_PATH}boards", "${request.method} ${request.target}")
        assertEquals("""{"title":"Trips","color":"00a15f"}""", request.body?.utf8())
        assertEquals("Trips", db.boardDao().get(ACCOUNT, 7)?.title)
        assertEquals(
            listOf("ana"),
            db.boardMemberDao().observeMembers(ACCOUNT, 7).first().map {
                it.uid
            }
        )
    }

    @Test
    fun `a new column goes after the last one`() = runTest {
        signedIn()
        server.enqueue(json("""{"id":20,"title":"Review","boardId":1,"order":1}"""))

        assertEquals(ApiResult.Success(Unit), creation.createColumn(BOARD, "Review"))

        assertEquals("""{"title":"Review","order":1}""", server.takeRequest().body?.utf8())
        assertEquals(
            listOf("To do", "Review"),
            db.stackDao().forBoard(ACCOUNT, BOARD).sortedBy {
                it.order
            }.map { it.title }
        )
    }

    @Test
    fun `offline nothing is created and the reason is kept`() = runTest {
        signedIn()
        server.enqueue(json("{}").newBuilder().onResponseStart(SocketEffect.CloseSocket()).build())
        server.enqueue(MockResponse(403))

        assertTrue(creation.createBoard("Trips", "00a15f") is ApiResult.NetworkError)
        assertEquals(ApiResult.HttpError(403), creation.createColumn(BOARD, "Review"))
        assertEquals(listOf("To do"), db.stackDao().forBoard(ACCOUNT, BOARD).map { it.title })
    }

    @Test
    fun `blank names or no account create nothing`() = runTest {
        signedIn()
        assertEquals(ApiResult.Unauthorized, creation.createBoard("  ", "fff"))
        every { session.activeAccount } returns flowOf(null)
        assertEquals(ApiResult.Unauthorized, creation.createColumn(BOARD, "Review"))
        assertEquals(0, server.requestCount)
    }
}
