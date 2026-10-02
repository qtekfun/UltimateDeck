// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.board

/**
 * Returns a copy of these columns with the item at [from] moved to [to].
 *
 * [to] is expressed against the columns *after* the item has been removed, so moving a card
 * down inside its own column uses the index it should end up at. A target index past the end
 * of the column is clamped to the end. The receiver is never modified.
 *
 * @throws IllegalArgumentException if [from] does not point at an item or [to] names a column
 * that does not exist.
 */
fun <T> List<List<T>>.moveItem(from: CardPosition, to: CardPosition): List<List<T>> {
    require(from.column in indices && from.index in this[from.column].indices) {
        "No item at $from"
    }
    require(to.column in indices) { "No column at index ${to.column}" }
    require(to.index >= 0) { "Negative target index ${to.index}" }

    val columns = map { it.toMutableList() }
    val item = columns[from.column].removeAt(from.index)
    val target = columns[to.column]
    target.add(to.index.coerceAtMost(target.size), item)
    return columns.map { it.toList() }
}
