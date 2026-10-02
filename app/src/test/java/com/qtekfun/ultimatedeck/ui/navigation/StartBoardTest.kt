// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import androidx.compose.ui.graphics.Color
import com.qtekfun.ultimatedeck.ui.boards.BoardSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartBoardTest {
    private val boards =
        listOf(
            BoardSummary(18, "Proyectos", Color.Green),
            BoardSummary(19, "Ultimatedeck", Color.Gray)
        )

    @Test
    fun `opens the favorite only while it is an active board`() {
        assertEquals(boards[1], startBoard(19, boards))
        assertEquals(null, startBoard(42, boards))
        assertEquals(null, startBoard(null, boards))
    }
}
