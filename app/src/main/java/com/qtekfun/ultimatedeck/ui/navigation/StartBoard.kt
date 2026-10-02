// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import com.qtekfun.ultimatedeck.ui.boards.BoardSummary

/** The board to open at start: the favorite, if it is still among the active boards. */
fun startBoard(favorite: Long?, boards: List<BoardSummary>): BoardSummary? =
    favorite?.let { id -> boards.firstOrNull { it.id == id } }
