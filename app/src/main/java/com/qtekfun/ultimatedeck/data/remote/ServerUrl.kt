// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * A validated Nextcloud server address. It can only be built by [parse], so an insecure or
 * malformed address never reaches the API client.
 */
class ServerUrl private constructor(val root: HttpUrl) {
    /** Base URL of the Deck API, v1.1. */
    val deckApi: HttpUrl get() = root.resolve(DECK_API_PATH)!!

    override fun toString(): String = root.toString()

    sealed interface ParseResult {
        data class Valid(val url: ServerUrl) : ParseResult

        /** Plain http: refused, HTTPS is mandatory (SPEC §6). */
        data object Insecure : ParseResult

        data object Invalid : ParseResult
    }

    companion object {
        private const val DECK_API_PATH = "index.php/apps/deck/api/v1.1/"

        /**
         * Parses what the user typed: `cloud.example.com`, `https://example.com/nextcloud/` or a
         * pasted `…/index.php` address. A missing scheme means https.
         */
        fun parse(input: String): ParseResult = parse(input, allowInsecure = false)

        /** [allowInsecure] exists only so tests can talk to a local plain-http server. */
        internal fun parse(input: String, allowInsecure: Boolean): ParseResult {
            val trimmed = input.trim()
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull()
            return when {
                url == null || url.host.isEmpty() -> ParseResult.Invalid
                !url.isHttps && !allowInsecure -> ParseResult.Insecure
                else -> ParseResult.Valid(ServerUrl(rootOf(url)))
            }
        }

        /**
         * Drops query, fragment and anything from `index.php` or `apps` on, so a Deck address
         * copied from the browser still gives the server root; the path ends with a slash.
         */
        private fun rootOf(url: HttpUrl): HttpUrl {
            val segments = url.pathSegments.filter { it.isNotEmpty() }
            val cut = segments.indexOfFirst { it == "index.php" || it == "apps" }
            val kept = if (cut >= 0) segments.take(cut) else segments
            val path = kept.joinToString(
                "/",
                prefix = "/",
                postfix = if (kept.isEmpty()) "" else "/"
            )
            return url.newBuilder().query(null).fragment(null).encodedPath(path).build()
        }
    }
}
