// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import com.qtekfun.ultimatedeck.ui.boards.BoardSummary

/** What the side menu can do: open a board, mark the favorite, open the settings. */
data class DrawerCallbacks(
    val onOpenBoard: (BoardSummary) -> Unit,
    val onFavorite: (boardId: Long?) -> Unit,
    val onSettings: () -> Unit
)
