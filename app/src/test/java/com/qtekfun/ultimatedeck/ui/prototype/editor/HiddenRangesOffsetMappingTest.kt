// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import com.qtekfun.ultimatedeck.domain.editor.SourceRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HiddenRangesOffsetMappingTest {
    // "**bold** x" with both "**" hidden -> "bold x"
    private val mapping =
        HiddenRangesOffsetMapping(listOf(SourceRange(0, 2), SourceRange(6, 8)), 10)

    @Test
    fun `maps original offsets around and inside hidden ranges`() {
        assertEquals(
            listOf(0, 0, 0, 1, 2, 3, 4, 4, 4, 5, 6),
            (0..10).map(mapping::originalToTransformed)
        )
    }

    @Test
    fun `maps transformed offsets to the original, before any hidden range at the boundary`() {
        assertEquals(listOf(0, 3, 4, 5, 6, 9, 10), (0..6).map(mapping::transformedToOriginal))
    }

    @Test
    fun `is the identity without hidden ranges`() {
        val identity = HiddenRangesOffsetMapping(emptyList(), 5)

        (0..5).forEach {
            assertEquals(it, identity.originalToTransformed(it))
            assertEquals(it, identity.transformedToOriginal(it))
        }
    }

    @Test
    fun `merges overlapping and touching ranges and drops empty ones`() {
        val merged = mergeRanges(
            listOf(
                SourceRange(5, 7),
                SourceRange(0, 2),
                SourceRange(2, 3),
                SourceRange(6, 9),
                SourceRange(4, 4)
            )
        )

        assertEquals(listOf(SourceRange(0, 3), SourceRange(5, 9)), merged)
    }
}
