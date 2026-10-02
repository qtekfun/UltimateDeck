// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.auth

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.auth.FakeCipher
import com.qtekfun.ultimatedeck.data.auth.LoginFlowApiFactory
import com.qtekfun.ultimatedeck.data.auth.loginFixture
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.ServerUrl
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Runs in real time with short intervals: virtual time would fire the login timeout while a real
 * HTTP request is still in flight.
 */
class LoginFlowTest {
    @StartStop
    val server = MockWebServer()

    private val nextcloud by lazy { FakeNextcloud(server).also { server.dispatcher = it } }
    private val db = inMemoryDatabase()
    private val store = CredentialStore(db, FakeCipher())
    private val session = AccountSession(db, store)
    private val flow = LoginFlow(LoginFlowApiFactory(OkHttpClient(), DeckJson), session)

    @AfterEach
    fun close() = db.close()

    private fun localServer(): ServerUrl {
        nextcloud
        val parsed = ServerUrl.parse(server.url("/nextcloud/").toString(), allowInsecure = true)
        return (parsed as ServerUrl.ParseResult.Valid).url
    }

    private fun states(timeoutMs: Long = 2_000) = runBlocking {
        flow.login(
            localServer(),
            pollInterval = 10.milliseconds,
            timeout = timeoutMs.milliseconds
        ).toList()
    }

    @Test
    fun `logs in through the browser and stores the account with encrypted credentials`() =
        runBlocking {
            val states = states()

            assertEquals(LoginState.CheckingServer, states[0])
            assertEquals(
                LoginState.WaitingForBrowser(server.url("/nextcloud/login/v2/flow/abc").toString()),
                states[1]
            )
            assertEquals(LoginState.Verifying, states[2])
            val accountId = (states[3] as LoginState.LoggedIn).accountId
            val account = session.activeAccount.first()!!
            assertEquals(accountId, account.id)
            assertEquals("https://cloud.example.com/", account.serverUrl)
            assertEquals("username", account.userId)
            assertEquals("username", session.credentials()?.loginName)
            assertEquals(session.credentials()?.appPassword, store.load(accountId)?.appPassword)
        }

    @Test
    fun `refuses plain http and malformed addresses without any request`() = runBlocking {
        assertEquals(
            listOf(LoginState.Failed(LoginError.INSECURE_URL)),
            flow.login("http://cloud.example.com").toList()
        )
        assertEquals(
            listOf(LoginState.Failed(LoginError.INVALID_URL)),
            flow.login("https://").toList()
        )
    }

    @Test
    fun `reports a server that is not Nextcloud`() {
        nextcloud.statusBody = """{"installed": false}"""
        assertEquals(LoginState.Failed(LoginError.NOT_NEXTCLOUD), states().last())

        nextcloud.statusBody = "<html>Welcome to nginx</html>"
        assertEquals(LoginState.Failed(LoginError.NOT_NEXTCLOUD), states().last())

        nextcloud.statusCode = 404
        assertEquals(LoginState.Failed(LoginError.NOT_NEXTCLOUD), states().last())
    }

    @Test
    fun `reports an unreachable server`() {
        val address = localServer()
        server.close()

        val states =
            runBlocking { flow.login(address, 10.milliseconds, 2_000.milliseconds).toList() }

        assertEquals(
            listOf(LoginState.CheckingServer, LoginState.Failed(LoginError.UNREACHABLE)),
            states
        )
    }

    @Test
    fun `reports Deck missing and revokes the new app password`() = runBlocking {
        nextcloud.capabilitiesBody = loginFixture("capabilities_no_deck.json")

        assertEquals(LoginState.Failed(LoginError.DECK_MISSING), states().last())
        assertTrue(nextcloud.requests.contains("DELETE /nextcloud/ocs/v2.php/core/apppassword"))
        assertNull(session.activeAccount.first())
    }

    @Test
    fun `reports a Deck without API v1 point 1 and revokes the app password`() {
        nextcloud.capabilitiesBody =
            loginFixture("capabilities_deck.json").replace("\"1.0\",\"1.1\"", "\"1.0\"")

        assertEquals(LoginState.Failed(LoginError.DECK_TOO_OLD), states().last())
        assertTrue(nextcloud.requests.contains("DELETE /nextcloud/ocs/v2.php/core/apppassword"))
    }

    @Test
    fun `expires when the user never finishes in the browser`() = runBlocking {
        nextcloud.pollsBeforeLogin = Int.MAX_VALUE

        val states = states(timeoutMs = 200)

        assertEquals(LoginState.Failed(LoginError.EXPIRED), states.last())
        assertTrue(nextcloud.requests.count { it.endsWith("/login/v2/poll") } > 1)
        assertNull(session.activeAccount.first())
    }

    @Test
    fun `cancelling stops the polling`() = runBlocking {
        nextcloud.pollsBeforeLogin = Int.MAX_VALUE

        val firstWaiting = flow.login(localServer(), 10.milliseconds, 60_000.milliseconds)
            .first { it is LoginState.WaitingForBrowser }
        val pollsAtCancel = nextcloud.requests.count { it.endsWith("/login/v2/poll") }
        Thread.sleep(100)

        assertTrue(firstWaiting is LoginState.WaitingForBrowser)
        assertEquals(pollsAtCancel, nextcloud.requests.count { it.endsWith("/login/v2/poll") })
    }

    @Test
    fun `upgrades an http poll endpoint on the server's host to https`() {
        val server = (
            ServerUrl.parse(
                "https://cloud.example.com/nextcloud"
            ) as ServerUrl.ParseResult.Valid
            ).url

        assertEquals(
            "https://cloud.example.com/nextcloud/login/v2/poll",
            LoginFlow.securePollEndpoint("http://cloud.example.com/nextcloud/login/v2/poll", server)
        )
        assertEquals(
            "http://elsewhere.example/poll",
            LoginFlow.securePollEndpoint("http://elsewhere.example/poll", server)
        )
        assertEquals("not a url", LoginFlow.securePollEndpoint("not a url", server))
    }
}
