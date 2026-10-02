// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ServerUrlTest {

    private fun valid(input: String) = (ServerUrl.parse(input) as ServerUrl.ParseResult.Valid).url

    @ParameterizedTest
    @CsvSource(
        "cloud.example.com, https://cloud.example.com/",
        "https://cloud.example.com, https://cloud.example.com/",
        "'  https://cloud.example.com/  ', https://cloud.example.com/",
        "https://example.com/nextcloud, https://example.com/nextcloud/",
        "https://example.com/nextcloud/index.php, https://example.com/nextcloud/",
        "https://example.com/index.php/apps/deck?x=1#top, https://example.com/",
        "https://example.com/nextcloud/apps/deck/#/board/3, https://example.com/nextcloud/",
        "https://example.com:8443/, https://example.com:8443/"
    )
    fun `normalizes what the user typed to the server root`(input: String, root: String) {
        assertEquals(root, valid(input).toString())
    }

    @ParameterizedTest
    @CsvSource(
        "cloud.example.com, https://cloud.example.com/index.php/apps/deck/api/v1.1/",
        "https://example.com/nextcloud/index.php, https://example.com/nextcloud/index.php/apps/deck/api/v1.1/"
    )
    fun `builds the Deck API base URL`(input: String, api: String) {
        assertEquals(api, valid(input).deckApi.toString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://cloud.example.com", "HTTP://cloud.example.com/nextcloud"])
    fun `refuses plain http`(input: String) {
        assertEquals(ServerUrl.ParseResult.Insecure, ServerUrl.parse(input))
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["", "   ", "https://", "ftp://cloud.example.com", "https://exa mple.com"]
    )
    fun `rejects malformed addresses`(input: String) {
        assertEquals(ServerUrl.ParseResult.Invalid, ServerUrl.parse(input))
    }
}
