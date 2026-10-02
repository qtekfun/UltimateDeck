// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult

private const val HTTP_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500

/**
 * What a server answer means for a queued operation. Network problems, 5xx, 408 and 429 are
 * temporary; a 401 too, since nothing must be lost while the session is fixed. Other 4xx and
 * unreadable answers wait for the user: retrying a create whose answer was lost could
 * duplicate the card.
 */
fun ApiResult<*>.toExecutionResult(): ExecutionResult = when (this) {
    is ApiResult.Success, ApiResult.NotModified -> ExecutionResult.Done()

    ApiResult.Unauthorized -> ExecutionResult.Retry("unauthorized")

    is ApiResult.NetworkError -> ExecutionResult.Retry("network: ${kind.name.lowercase()}")

    is ApiResult.HttpError -> if (isTemporary(code)) {
        ExecutionResult.Retry("HTTP $code")
    } else {
        ExecutionResult.Failed("HTTP $code")
    }

    ApiResult.NotFound -> ExecutionResult.Failed("not found")

    ApiResult.ParseError -> ExecutionResult.Failed("unreadable response")
}

private fun isTemporary(code: Int) =
    code >= HTTP_SERVER_ERROR || code == HTTP_TIMEOUT || code == HTTP_TOO_MANY_REQUESTS
