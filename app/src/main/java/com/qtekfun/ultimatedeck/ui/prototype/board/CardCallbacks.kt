// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

/** What a card can ask the board to do: move to another column or open its details. */
data class CardCallbacks(
    val onMoveToColumn: (cardId: Long, column: Int) -> Unit,
    val onOpen: (cardId: Long) -> Unit
)
