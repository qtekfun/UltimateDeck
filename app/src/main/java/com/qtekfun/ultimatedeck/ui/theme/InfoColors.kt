// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Added to both luminances by the WCAG contrast formula. */
private const val CONTRAST_OFFSET = 0.05f

/** WCAG contrast ratio between two opaque colors, from 1 to 21. */
fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance() + CONTRAST_OFFSET
    val b = second.luminance() + CONTRAST_OFFSET
    return maxOf(a, b) / minOf(a, b)
}

/**
 * Black or white, whichever reads better on [background]. Label and board colors come from the
 * server and can be anything; one of the two always reaches 4.5:1.
 */
fun contentColorOn(background: Color): Color =
    if (contrastRatio(background, Color.Black) >= contrastRatio(background, Color.White)) {
        Color.Black
    } else {
        Color.White
    }

internal val PersonColors = listOf(
    Color(0xFFB45309),
    Color(0xFF0F766E),
    Color(0xFF6D3FC4),
    Color(0xFFBE185D),
    Color(0xFF1D4ED8),
    Color(0xFF15803D),
    Color(0xFFB91C1C),
    Color(0xFF475569)
)

/** The color of a person: the same one on every card and in every session. */
fun personColor(name: String): Color = PersonColors[name.hashCode().mod(PersonColors.size)]
