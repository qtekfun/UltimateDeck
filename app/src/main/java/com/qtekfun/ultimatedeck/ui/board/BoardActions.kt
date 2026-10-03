// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

/** Where the board screen can take the user: the menu, a card, archived cards, a new column. */
data class BoardActions(
    val onMenu: () -> Unit,
    val onOpenCard: (cardId: Long) -> Unit,
    val onShowArchived: () -> Unit,
    val onAddColumn: () -> Unit
)
