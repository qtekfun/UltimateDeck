// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val READABLE = 4.5f

class InfoColorsTest {
    @Test
    fun `text is black on light colors and white on dark ones`() {
        assertEquals(Color.Black, contentColorOn(Color.Yellow))
        assertEquals(Color.White, contentColorOn(Color(0xFF1A237E)))
    }

    @Test
    fun `text is readable on any server color`() {
        val steps = 0..255 step 15
        val unreadable = steps.flatMap { r ->
            steps.flatMap { g -> steps.map { b -> Color(r, g, b) } }
        }.filter { contrastRatio(it, contentColorOn(it)) < READABLE }

        assertTrue(unreadable.isEmpty()) { "Unreadable on $unreadable" }
    }

    @Test
    fun `a person keeps their color and every one is readable`() {
        assertEquals(personColor("Mia Chen"), personColor("Mia Chen"))
        assertTrue(personColor("") in PersonColors)
        assertTrue(PersonColors.all { contrastRatio(it, Color.White) >= READABLE })
    }
}
