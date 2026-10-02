// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.auth

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.auth.FakeCipher
import com.qtekfun.ultimatedeck.data.auth.LoginFlowApiFactory
import com.qtekfun.ultimatedeck.data.local.Fixtures
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.remote.ServerUrl
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LogoutTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val store = CredentialStore(db, FakeCipher())
    private val session = AccountSession(db, store)
    private val logout = Logout(LoginFlowApiFactory(OkHttpClient(), DeckJson), session, store, db)

    @AfterEach
    fun close() = db.close()

    /** Signs in an account whose server is the local MockWebServer, with a board and a card. */
    private suspend fun signedIn(): Long {
        val url = (
            ServerUrl.parse(
                server.url("/nextcloud/").toString(),
                allowInsecure = true
            ) as ServerUrl.ParseResult.Valid
            )
        val accountId = session.signIn(url.url, Credentials("ana", "app-password"))
        db.boardDao().upsert(listOf(Fixtures.board(accountId)))
        db.stackDao().upsert(listOf(Fixtures.stack(accountId)))
        db.cardDao().upsert(listOf(Fixtures.card(accountId)))
        return accountId
    }

    @Test
    fun `revokes the app password and deletes the account with all its data`() = runBlocking {
        server.enqueue(MockResponse(200))
        val accountId = signedIn()

        logout.run(allowInsecure = true)

        val request = server.takeRequest()
        assertEquals(
            "DELETE /nextcloud/ocs/v2.php/core/apppassword",
            "${request.method} ${request.target}"
        )
        val basic = request.headers["Authorization"].orEmpty().removePrefix("Basic ")
        assertEquals("ana:app-password", String(Base64.getDecoder().decode(basic)))
        assertNull(session.activeAccount.first())
        assertNull(session.credentials())
        assertNull(store.load(accountId))
        assertNull(db.cardDao().get(accountId, 100))
    }

    @Test
    fun `still logs out when the server cannot be reached`() = runBlocking {
        val accountId = signedIn()
        server.close()

        logout.run(allowInsecure = true)

        assertNull(session.activeAccount.first())
        assertNull(store.load(accountId))
    }

    @Test
    fun `uses the stored credentials after an app restart`() = runBlocking {
        server.enqueue(MockResponse(200))
        signedIn()
        session.forget()

        logout.run(allowInsecure = true)

        assertEquals(true, server.takeRequest().headers["Authorization"]?.startsWith("Basic "))
    }

    @Test
    fun `does nothing without an account`() = runBlocking {
        logout()

        assertNull(session.activeAccount.first())
    }
}
