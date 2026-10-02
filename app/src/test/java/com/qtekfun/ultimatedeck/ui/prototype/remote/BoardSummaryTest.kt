// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoardSummaryTest {
    @Test
    fun `falls back to the Nextcloud blue for unusable colors`() {
        assertEquals(Color(0xFF0082C9), deckColor(""))
        assertEquals(Color(0xFF0082C9), deckColor("zzzzzz"))
        assertEquals(Color(0xFFFF0000), deckColor("#ff0000"))
    }
}
