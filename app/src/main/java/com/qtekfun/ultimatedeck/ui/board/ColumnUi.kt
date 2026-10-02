// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.domain.board.moveItem

/** A board column as shown on the board. */
data class ColumnUi(val id: Long, val title: String, val cards: List<CardUi>)

/** Applies a card move to the columns, keeping column metadata untouched. */
fun List<ColumnUi>.withCardMoved(from: CardPosition, to: CardPosition): List<ColumnUi> {
    val cards = map { it.cards }.moveItem(from, to)
    return mapIndexed { index, column -> column.copy(cards = cards[index]) }
}

/** Position of the card with [cardId], or null if no column holds it. */
fun List<ColumnUi>.positionOf(cardId: Long): CardPosition? {
    forEachIndexed { columnIndex, column ->
        val index = column.cards.indexOfFirst { it.id == cardId }
        if (index >= 0) return CardPosition(columnIndex, index)
    }
    return null
}
