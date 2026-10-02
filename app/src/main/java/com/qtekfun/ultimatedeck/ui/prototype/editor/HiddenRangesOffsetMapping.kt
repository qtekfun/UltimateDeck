// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.ui.text.input.OffsetMapping
import com.qtekfun.ultimatedeck.domain.editor.SourceRange

/**
 * Offset mapping for a text where [hidden] ranges of the original are not displayed.
 * [hidden] must be sorted and non-overlapping; see [mergeRanges].
 */
internal class HiddenRangesOffsetMapping(
    private val hidden: List<SourceRange>,
    private val originalLength: Int
) : OffsetMapping {
    private val transformedLength = originalLength - hidden.sumOf { it.length }

    override fun originalToTransformed(offset: Int): Int {
        var removed = 0
        for (range in hidden) {
            if (range.start >= offset) break
            removed += minOf(range.end, offset) - range.start
        }
        return (offset - removed).coerceIn(0, transformedLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        var original = offset
        for (range in hidden) {
            if (range.start < original) original += range.length else break
        }
        return original.coerceIn(0, originalLength)
    }
}

/** Sorts [ranges] and merges the ones that overlap or touch. Empty ranges are dropped. */
internal fun mergeRanges(ranges: List<SourceRange>): List<SourceRange> =
    ranges.filter { it.length > 0 }
        .sortedBy { it.start }
        .fold(mutableListOf()) { merged, range ->
            val last = merged.lastOrNull()
            if (last != null && range.start <= last.end) {
                merged[merged.lastIndex] = SourceRange(last.start, maxOf(last.end, range.end))
            } else {
                merged += range
            }
            merged
        }
