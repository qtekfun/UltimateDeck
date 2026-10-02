// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.API_PATH
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiFixtures
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.remote.json
import com.qtekfun.ultimatedeck.data.remote.testDeckApi
import com.qtekfun.ultimatedeck.sync.queue.FixedRandom
import com.qtekfun.ultimatedeck.sync.queue.MutableClock
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.ProcessResult
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncEngineTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val queue = OperationQueue(db, MutableClock(), FixedRandom(0.5))
    private val session = mockk<AccountSession>(relaxed = true)
    private val apiProvider = mockk<AccountApiProvider>()
    private val engine = SyncEngine(session, apiProvider, db, queue, PullSync(db), Dispatchers.IO)

    @AfterEach
    fun close() = db.close()

    private suspend fun signedIn() {
        db.seedBoard()
        every { session.credentials() } returns Credentials("ana", "secret")
        every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
        coEvery { apiProvider.api() } returns testDeckApi(server)
    }

    private fun targets() = List(server.requestCount) {
        server.takeRequest().let { "${it.method} ${it.target}" }
    }

    @Test
    fun `without an account nothing is synced`() = runTest {
        every { session.credentials() } returns null
        every { session.activeAccount } returns flowOf(null)

        assertEquals(SyncOutcome.NoAccount, engine.sync())
        coVerify { session.restore() }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `local changes are sent before pulling`() = runTest {
        signedIn()
        db.cardDao().upsert(listOf(card(5)))
        queue.enqueue(ACCOUNT, 5, QueuedOperation.ArchiveCard(BOARD, STACK, archived = true))
        server.enqueue(json(ApiFixtures.read("card_created.json")))
        server.enqueue(MockResponse(304))
        server.enqueue(json("[]"))

        val outcome = engine.sync()

        assertEquals(SyncOutcome.Ok(ProcessResult(done = 1)), outcome)
        assertEquals(
            listOf(
                "PUT ${API_PATH}boards/1/stacks/10/cards/5/archive",
                "GET ${API_PATH}boards?details=true",
                "GET ${API_PATH}boards/1/stacks"
            ),
            targets()
        )
    }

    @Test
    fun `two syncs at once run one after the other`() = runTest {
        signedIn()
        server.enqueue(
            MockResponse(304).newBuilder().headersDelay(200, TimeUnit.MILLISECONDS).build()
        )
        repeat(3) { server.enqueue(MockResponse(304)) }

        val first = async { engine.sync() }
        val second = async { engine.sync() }

        assertEquals(SyncOutcome.Ok(ProcessResult()), first.await())
        assertEquals(SyncOutcome.Ok(ProcessResult()), second.await())
        assertEquals(
            listOf(
                "boards?details=true",
                "boards/1/stacks",
                "boards?details=true",
                "boards/1/stacks"
            ),
            targets().map { it.substringAfter(API_PATH) }
        )
    }

    @Test
    fun `a lost connection is reported as offline`() = runTest {
        signedIn()
        server.enqueue(json("[]").newBuilder().onResponseStart(SocketEffect.CloseSocket()).build())

        assertEquals(null, engine.lastOutcome.value)
        assertEquals(SyncOutcome.Offline, engine.sync())
        assertEquals(SyncOutcome.Offline, engine.lastOutcome.value)
    }

    @Test
    fun `server refusals are reported`() = runTest {
        signedIn()
        server.enqueue(MockResponse(401))
        server.enqueue(MockResponse(500))
        server.enqueue(json("not json"))

        assertEquals(SyncOutcome.Unauthorized, engine.sync())
        assertEquals(SyncOutcome.Error("HTTP 500"), engine.sync())
        assertEquals(SyncOutcome.Error("ParseError"), engine.sync())
    }
}
