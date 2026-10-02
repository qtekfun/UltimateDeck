// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import java.util.Base64
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeckApiFactoryTest {
    @StartStop
    val server = MockWebServer()

    private fun apiFor(credentials: Credentials?): DeckApi {
        val url = ServerUrl.parse(server.url("/").toString(), allowInsecure = true)
        return DeckApiFactory(OkHttpClient(), DeckJson)
            .create((url as ServerUrl.ParseResult.Valid).url) { credentials }
    }

    @Test
    fun `sends basic auth, the OCS header and JSON accept to the Deck API path`() = runTest {
        server.enqueue(json("[]"))

        val result = apiCall { apiFor(Credentials("ana", "app-pässword")).boards.getBoards() }

        val request = server.takeRequest()
        assertEquals("${API_PATH}boards?details=true", request.target)
        assertEquals("true", request.headers["OCS-APIRequest"])
        assertEquals("application/json", request.headers["Accept"])
        val basic = request.headers["Authorization"].orEmpty().removePrefix("Basic ")
        assertEquals("ana:app-pässword", String(Base64.getDecoder().decode(basic), Charsets.UTF_8))
        assertEquals(ApiResult.Success(emptyList<Any>()), result)
    }

    @Test
    fun `sends no authorization without credentials`() = runTest {
        server.enqueue(json("[]"))

        apiCall { apiFor(null).boards.getBoards() }

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `never prints the app password`() {
        val text = Credentials("ana", "s3cret-app-password").toString()

        assertFalse(text.contains("s3cret"))
        assertEquals("Credentials(loginName=ana, appPassword=***)", text)
    }
}
