// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.qtekfun.ultimatedeck.domain.board.CardPosition

/** The card being dragged, where it came from and where the finger grabbed it. */
data class DraggedCard(
    val card: CardUi,
    val from: CardPosition,
    val grabOffset: Offset,
    val size: Size
)

/**
 * Drag and drop state of a board. All geometry is in root coordinates; [boardOrigin] converts
 * it to the board's local space, where the pointer and the floating card live.
 */
@Stable
class BoardDragState {
    var dragged: DraggedCard? by mutableStateOf(null)
        private set

    /** Pointer position in board coordinates. */
    var pointer: Offset by mutableStateOf(Offset.Zero)
        private set

    var target: CardPosition? by mutableStateOf(null)
        private set

    val isDragging: Boolean get() = dragged != null

    var boardOrigin: Offset = Offset.Zero
    val cardBounds = mutableMapOf<Long, Rect>()
    val columnBounds = mutableMapOf<Int, Rect>()
    val columnListStates = mutableMapOf<Int, LazyListState>()

    /** Starts dragging the card under [position], if any. Returns whether a drag started. */
    fun start(position: Offset, columns: List<ColumnUi>): Boolean {
        val rootPosition = position + boardOrigin
        val hit = cardBounds.entries.firstOrNull { it.value.contains(rootPosition) }
        val from = hit?.let { columns.positionOf(it.key) }
        if (hit == null || from == null) return false
        dragged = DraggedCard(
            card = columns[from.column].cards[from.index],
            from = from,
            grabOffset = rootPosition - hit.value.topLeft,
            size = hit.value.size
        )
        pointer = position
        target = from
        return true
    }

    fun moveTo(position: Offset, columns: List<ColumnUi>) {
        pointer = position
        updateTarget(columns)
    }

    /** Recomputes the drop target from the center of the floating card. */
    fun updateTarget(columns: List<ColumnUi>) {
        val card = dragged ?: return
        val center = pointer + boardOrigin - card.grabOffset +
            Offset(card.size.width / 2, card.size.height / 2)
        val column = columnAt(
            center.x,
            columnBounds.mapValues { (_, bounds) -> bounds.left..bounds.right }
        ) ?: return
        val centers = columns[column].cards
            .filter { it.id != card.card.id }
            .map { cardBounds[it.id]?.center?.y }
        target = CardPosition(column, dropIndex(center.y, centers))
    }

    /** Ends the drag and returns the move to apply, or null if nothing was being dragged. */
    fun drop(): Pair<CardPosition, CardPosition>? {
        val move = dragged?.let { card -> target?.let { card.from to it } }
        cancel()
        return move
    }

    fun cancel() {
        dragged = null
        target = null
    }

    /** Columns as they look while dragging: the dragged card sits at the current target. */
    fun preview(columns: List<ColumnUi>): List<ColumnUi> {
        val card = dragged
        val to = target
        return if (card != null && to != null) columns.withCardMoved(card.from, to) else columns
    }
}
