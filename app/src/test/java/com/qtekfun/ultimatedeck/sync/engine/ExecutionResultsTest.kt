// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecutionResultsTest {
    @Test
    fun `successful answers are done`() {
        assertEquals(ExecutionResult.Done(), ApiResult.Success(Unit).toExecutionResult())
        assertEquals(ExecutionResult.Done(), ApiResult.NotModified.toExecutionResult())
    }

    @Test
    fun `temporary problems are retried`() {
        val retried = listOf(
            ApiResult.Unauthorized to "unauthorized",
            ApiResult.NetworkError(ApiResult.NetworkError.Kind.TIMEOUT) to "network: timeout",
            ApiResult.HttpError(500) to "HTTP 500",
            ApiResult.HttpError(408) to "HTTP 408",
            ApiResult.HttpError(429) to "HTTP 429"
        )

        retried.forEach { (result, reason) ->
            assertEquals(ExecutionResult.Retry(reason), result.toExecutionResult())
        }
    }

    @Test
    fun `refusals and unreadable answers wait for the user`() {
        assertEquals(
            ExecutionResult.Failed("HTTP 400"),
            ApiResult.HttpError(400).toExecutionResult()
        )
        assertEquals(ExecutionResult.Failed("not found"), ApiResult.NotFound.toExecutionResult())
        assertEquals(
            ExecutionResult.Failed("unreadable response"),
            ApiResult.ParseError.toExecutionResult()
        )
    }
}
