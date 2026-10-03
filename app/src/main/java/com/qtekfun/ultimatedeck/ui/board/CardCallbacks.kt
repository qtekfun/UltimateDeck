// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

/** What a column can ask the board to do: move a card, open its details or add a new one. */
data class CardCallbacks(
    val onMoveToColumn: (cardId: Long, column: Int) -> Unit,
    val onOpen: (cardId: Long) -> Unit,
    val onAddCard: (columnId: Long) -> Unit,
    /** Null unless deleting is turned on in Settings (T15d). */
    val onDeleteColumn: ((columnId: Long) -> Unit)? = null
)
