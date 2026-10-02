// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** How a part of a description is edited: as styled text, as a table grid, as code or verbatim. */
enum class SegmentKind { TEXT, TABLE, CODE, RAW }

/** A run of blocks edited by the same kind of editor, as an exact slice of the source. */
data class DocumentSegment(val kind: SegmentKind, val range: SourceRange)

/**
 * Splits a document into editor segments. Consecutive text-like blocks (paragraphs, headings,
 * lists, quotes, blank lines...) merge into one [SegmentKind.TEXT] segment so they can be edited
 * in a single field; tables, code and HTML get their own segment.
 */
object DocumentSegments {

    fun split(document: MarkdownDocument): List<DocumentSegment> {
        val segments = mutableListOf<DocumentSegment>()
        document.blocks.forEach { block ->
            val kind = segmentKind(block.kind)
            val last = segments.lastOrNull()
            if (kind == SegmentKind.TEXT && last?.kind == SegmentKind.TEXT) {
                segments[segments.lastIndex] =
                    last.copy(range = SourceRange(last.range.start, block.range.end))
            } else {
                segments += DocumentSegment(kind, block.range)
            }
        }
        return segments
    }

    /** The source with [segment] replaced by [text]; everything else is kept byte for byte. */
    fun replace(source: String, segment: DocumentSegment, text: String): String =
        source.substring(0, segment.range.start) + text + source.substring(segment.range.end)

    private fun segmentKind(kind: BlockKind): SegmentKind = when (kind) {
        BlockKind.TABLE -> SegmentKind.TABLE
        BlockKind.CODE -> SegmentKind.CODE
        BlockKind.HTML -> SegmentKind.RAW
        else -> SegmentKind.TEXT
    }
}
