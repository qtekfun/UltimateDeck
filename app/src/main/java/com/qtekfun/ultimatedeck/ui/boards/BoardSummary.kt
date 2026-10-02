// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.boards

import androidx.compose.ui.graphics.Color

/** A board in the list, with its color ready to draw. */
data class BoardSummary(val id: Long, val title: String, val color: Color)

private const val OPAQUE = 0xFF000000
private const val NEXTCLOUD_BLUE = 0xFF0082C9
private const val RGB_DIGITS = 6
private const val HEX = 16

/** Deck colors are hex RGB without '#'; anything else falls back to the Nextcloud blue. */
fun deckColor(hex: String): Color = hex.removePrefix("#")
    .takeIf { it.length == RGB_DIGITS }
    ?.toLongOrNull(HEX)
    ?.let { Color(OPAQUE or it) }
    ?: Color(NEXTCLOUD_BLUE)
