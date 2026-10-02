// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.dto

import kotlinx.serialization.json.Json

/**
 * JSON settings for the Deck API. Reading: unknown fields are ignored and nulls fall back to
 * defaults, so newer or older servers still parse. Writing: defaults are sent (Deck requires
 * fields such as the card type) and null optional fields are left out.
 */
val DeckJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
}
