// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import mockwebserver3.junit5.StartStop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ApiCallTest {
    @StartStop
    val server = MockWebServer()

    private val api by lazy { testDeckApi(server) }

    private suspend fun boards() = apiCall { api.boards.getBoards() }

    @Test
    fun `returns the parsed body and its ETag`() = runTest {
        server.enqueue(json(ApiFixtures.read("boards.json"), 200, "ETag", "\"abc\""))

        val result = boards()

        assertInstanceOf(ApiResult.Success::class.java, result)
        result as ApiResult.Success
        assertEquals(listOf(10L, 11L), result.value.map { it.id })
        assertEquals("\"abc\"", result.etag)
    }

    @Test
    fun `maps 304 to not modified`() = runTest {
        server.enqueue(MockResponse(304))

        assertEquals(ApiResult.NotModified, boards())
    }

    @ParameterizedTest
    @CsvSource("401, Unauthorized", "404, NotFound")
    fun `maps auth and missing errors to their own results`(code: Int, expected: String) = runTest {
        server.enqueue(json("{}", code))

        assertEquals(expected, boards()::class.simpleName)
    }

    @ParameterizedTest
    @CsvSource("400", "403", "500", "503")
    fun `maps other client and server errors to HttpError`(code: Int) = runTest {
        server.enqueue(json("{\"message\":\"nope\"}", code))

        assertEquals(ApiResult.HttpError(code), boards())
    }

    @Test
    fun `maps a slow server to a timeout`() = runTest {
        server.enqueue(json("[]").newBuilder().headersDelay(2, TimeUnit.SECONDS).build())

        assertEquals(ApiResult.NetworkError(ApiResult.NetworkError.Kind.TIMEOUT), boards())
    }

    @Test
    fun `maps a server that is down to unreachable`() = runTest {
        val apiOfClosedServer = testDeckApi(server)
        server.close()

        assertEquals(
            ApiResult.NetworkError(ApiResult.NetworkError.Kind.UNREACHABLE),
            apiCall { apiOfClosedServer.boards.getBoards() }
        )
    }

    @Test
    fun `maps a connection dropped mid-response to a network error`() = runTest {
        server.enqueue(json("[]").newBuilder().onResponseStart(SocketEffect.CloseSocket()).build())

        assertInstanceOf(ApiResult.NetworkError::class.java, boards())
    }

    @Test
    fun `maps unexpected JSON to a parse error`() = runTest {
        server.enqueue(json("{\"not\": \"a list\"}"))

        assertEquals(ApiResult.ParseError, boards())
    }

    @Test
    fun `maps a successful empty body to a parse error when content was expected`() = runTest {
        server.enqueue(MockResponse(204))

        assertEquals(ApiResult.ParseError, boards())
    }
}
