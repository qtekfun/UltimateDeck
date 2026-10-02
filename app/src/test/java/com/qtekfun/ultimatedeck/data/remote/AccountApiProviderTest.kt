// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.auth.FakeCipher
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountApiProviderTest {
    @StartStop
    val server = MockWebServer()

    private val db = inMemoryDatabase()
    private val session = AccountSession(db, CredentialStore(db, FakeCipher()))
    private val provider = AccountApiProvider(DeckApiFactory(OkHttpClient(), DeckJson), session)

    @AfterEach
    fun close() = db.close()

    @Test
    fun `has no API without a signed-in account`() = runBlocking {
        assertNull(provider.api())
    }

    @Test
    fun `calls the account's server with its credentials`() = runBlocking {
        val url = ServerUrl.parse(
            server.url("/").toString(),
            allowInsecure = true
        ) as ServerUrl.ParseResult.Valid
        session.signIn(url.url, Credentials("ana", "app-password"))
        server.enqueue(json("[]"))

        apiCall { provider.api(allowInsecure = true)!!.boards.getBoards() }

        val request = server.takeRequest()
        assertEquals("${API_PATH}boards?details=true", request.target)
        assertEquals(basicHeader("ana", "app-password"), request.headers["Authorization"])
    }

    @Test
    fun `refuses an insecure stored address outside tests`() = runBlocking {
        val url = ServerUrl.parse(
            server.url("/").toString(),
            allowInsecure = true
        ) as ServerUrl.ParseResult.Valid
        session.signIn(url.url, Credentials("ana", "app-password"))

        assertNull(provider.api())
    }

    private fun basicHeader(user: String, password: String) =
        okhttp3.Credentials.basic(user, password)
}
