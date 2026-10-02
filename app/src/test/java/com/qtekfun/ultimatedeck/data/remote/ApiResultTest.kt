// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiResultTest {
    @Test
    fun `map transforms only successes and keeps the ETag`() {
        assertEquals(ApiResult.Success(4, "e"), ApiResult.Success(2, "e").map { it * 2 })
        val failures = listOf(
            ApiResult.NotModified,
            ApiResult.Unauthorized,
            ApiResult.NotFound,
            ApiResult.HttpError(500),
            ApiResult.NetworkError(ApiResult.NetworkError.Kind.TIMEOUT),
            ApiResult.ParseError
        )
        failures.forEach { assertEquals(it, (it as ApiResult<Int>).map { value -> value * 2 }) }
    }
}
