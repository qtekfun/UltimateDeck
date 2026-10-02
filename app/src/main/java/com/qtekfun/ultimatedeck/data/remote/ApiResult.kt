// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

/** Outcome of a Deck API call. Network and HTTP failures are values, never exceptions. */
sealed interface ApiResult<out T> {
    /** [etag] is the response ETag, to send back as If-None-Match next time. */
    data class Success<T>(val value: T, val etag: String? = null) : ApiResult<T>

    /** 304: nothing changed since the ETag or date sent. */
    data object NotModified : ApiResult<Nothing>

    /** 401: the app password is wrong or was revoked. */
    data object Unauthorized : ApiResult<Nothing>

    /** 404: the entity no longer exists on the server. */
    data object NotFound : ApiResult<Nothing>

    /** Any other 4xx or 5xx response. */
    data class HttpError(val code: Int) : ApiResult<Nothing>

    data class NetworkError(val kind: Kind) : ApiResult<Nothing> {
        enum class Kind { TIMEOUT, UNREACHABLE, TLS, OTHER }
    }

    /** The server answered something that is not the expected JSON. */
    data object ParseError : ApiResult<Nothing>
}
