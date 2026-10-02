// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.annotation.StringRes
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.domain.board.moveItem

/** Fake board column for the drag and drop prototype (T02). */
data class PrototypeColumn(
    val id: Long,
    @param:StringRes val title: Int,
    val cards: List<PrototypeCard>
)

/** Applies a card move to the columns, keeping column metadata untouched. */
fun List<PrototypeColumn>.withCardMoved(
    from: CardPosition,
    to: CardPosition
): List<PrototypeColumn> {
    val cards = map { it.cards }.moveItem(from, to)
    return mapIndexed { index, column -> column.copy(cards = cards[index]) }
}

/** Position of the card with [cardId], or null if no column holds it. */
fun List<PrototypeColumn>.positionOf(cardId: Long): CardPosition? {
    forEachIndexed { columnIndex, column ->
        val index = column.cards.indexOfFirst { it.id == cardId }
        if (index >= 0) return CardPosition(columnIndex, index)
    }
    return null
}
