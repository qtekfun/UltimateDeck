// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

/**
 * Index at which a card dragged to vertical position [y] should be dropped, given the vertical
 * centers of the other cards of the column in order. A null center is a card that is not laid
 * out (scrolled away): before the first visible card it counts as above, after it as below.
 */
internal fun dropIndex(y: Float, centers: List<Float?>): Int {
    var index = 0
    var seenVisible = false
    for (center in centers) {
        val above = if (center == null) !seenVisible else center < y
        if (!above) break
        if (center != null) seenVisible = true
        index++
    }
    return index
}

/**
 * Column under horizontal position [x], or the closest one if [x] falls in a gap or outside.
 * [columns] maps column indexes to their horizontal extent; null if it is empty.
 */
internal fun columnAt(x: Float, columns: Map<Int, ClosedFloatingPointRange<Float>>): Int? =
    columns.entries.firstOrNull { x in it.value }?.key
        ?: columns.minByOrNull { (_, range) ->
            if (x < range.start) range.start - x else x - range.endInclusive
        }?.key

/**
 * Auto-scroll speed for a pointer at [position] inside the range [start]..[end]: negative near
 * the start, positive near the end, zero elsewhere. It grows linearly from zero at [edge] pixels
 * from the border up to [maxSpeed] at the border and beyond.
 */
internal fun autoScrollVelocity(
    position: Float,
    start: Float,
    end: Float,
    edge: Float,
    maxSpeed: Float
): Float = when {
    position < start + edge -> -maxSpeed * ((start + edge - position) / edge).coerceAtMost(1f)
    position > end - edge -> maxSpeed * ((position - (end - edge)) / edge).coerceAtMost(1f)
    else -> 0f
}
