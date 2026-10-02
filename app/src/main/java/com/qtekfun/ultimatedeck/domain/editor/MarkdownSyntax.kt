// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** Kind of a top-level markdown block. */
enum class BlockKind {
    PARAGRAPH,
    HEADING,
    BULLET_LIST,
    ORDERED_LIST,
    QUOTE,
    CODE,
    TABLE,
    HTML,
    RULE,
    LINK_DEFINITION,
    BLANK,
    OTHER
}

/** A top-level block and the exact slice of the source it comes from. */
data class MarkdownBlock(val kind: BlockKind, val range: SourceRange)

/** Kind of an inline span that the editor styles. */
enum class InlineKind { STRONG, EMPHASIS, STRIKETHROUGH, CODE, LINK }

/** An inline span: [range] covers the whole span, [markers] the syntax characters inside it. */
data class InlineSpan(val kind: InlineKind, val range: SourceRange, val markers: List<SourceRange>)

/** A heading line: its [level], full [range] and the [marker] syntax (`#` signs or underline). */
data class HeadingLine(val level: Int, val range: SourceRange, val marker: SourceRange)

/** A task list checkbox: [range] covers the three characters `[ ]` or `[x]`. */
data class TaskMarker(val range: SourceRange, val checked: Boolean)

/** Everything the live editor styles, found in one walk of the syntax tree. */
data class MarkdownSyntaxIndex(
    val inlineSpans: List<InlineSpan>,
    val headings: List<HeadingLine>,
    val tasks: List<TaskMarker>,
    val bullets: List<SourceRange>,
    val monospaceBlocks: List<SourceRange>,
    val quotes: List<SourceRange> = emptyList(),
    val quoteMarkers: List<SourceRange> = emptyList()
)
