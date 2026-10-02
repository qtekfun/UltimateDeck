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

/** Transforms a successful value; failures pass through unchanged. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value), etag)
    ApiResult.NotModified -> ApiResult.NotModified
    ApiResult.Unauthorized -> ApiResult.Unauthorized
    ApiResult.NotFound -> ApiResult.NotFound
    is ApiResult.HttpError -> this
    is ApiResult.NetworkError -> this
    ApiResult.ParseError -> ApiResult.ParseError
}
