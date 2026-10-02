// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

/**
 * Read-only analysis of a markdown [source]. It never rewrites the source: blocks are exact
 * slices of it, so [toMarkdown] always returns [source] unchanged, and edits are applied as
 * minimal text changes by [MarkdownEdits].
 */
class MarkdownDocument private constructor(
    val source: String,
    val blocks: List<MarkdownBlock>,
    private val syntax: MarkdownSyntaxIndex
) {
    val inlineSpans: List<InlineSpan> get() = syntax.inlineSpans
    val headings: List<HeadingLine> get() = syntax.headings
    val tasks: List<TaskMarker> get() = syntax.tasks
    val bullets: List<SourceRange> get() = syntax.bullets
    val monospaceBlocks: List<SourceRange> get() = syntax.monospaceBlocks

    /** Rebuilds the markdown from the blocks; equal to [source] by construction. */
    fun toMarkdown(): String = blocks.joinToString("") {
        source.substring(it.range.start, it.range.end)
    }

    companion object {
        private val parser = MarkdownParser(
            GFMFlavourDescriptor(),
            true,
            CancellationToken.NonCancellable
        )

        fun parse(source: String): MarkdownDocument {
            val root = parser.buildMarkdownTreeFromString(source as CharSequence)
            val collector = SyntaxCollector(source)
            collector.visit(root)
            return MarkdownDocument(source, topLevelBlocks(root, source.length), collector.index())
        }

        /** Top-level blocks, with any gap between parser nodes filled so they cover the source. */
        private fun topLevelBlocks(root: ASTNode, length: Int): List<MarkdownBlock> {
            val blocks = mutableListOf<MarkdownBlock>()
            var cursor = 0
            for (node in root.children.sortedBy { it.startOffset }) {
                if (node.startOffset > cursor) {
                    blocks += MarkdownBlock(BlockKind.BLANK, SourceRange(cursor, node.startOffset))
                }
                if (node.endOffset > maxOf(cursor, node.startOffset)) {
                    val start = maxOf(cursor, node.startOffset)
                    blocks +=
                        MarkdownBlock(blockKind(node.type), SourceRange(start, node.endOffset))
                    cursor = node.endOffset
                }
            }
            if (cursor <
                length
            ) {
                blocks += MarkdownBlock(BlockKind.BLANK, SourceRange(cursor, length))
            }
            return blocks
        }

        private fun blockKind(type: IElementType): BlockKind = when (type) {
            MarkdownElementTypes.PARAGRAPH -> BlockKind.PARAGRAPH
            in HEADING_LEVELS -> BlockKind.HEADING
            MarkdownElementTypes.UNORDERED_LIST -> BlockKind.BULLET_LIST
            MarkdownElementTypes.ORDERED_LIST -> BlockKind.ORDERED_LIST
            MarkdownElementTypes.BLOCK_QUOTE -> BlockKind.QUOTE
            MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> BlockKind.CODE
            GFMElementTypes.TABLE -> BlockKind.TABLE
            MarkdownElementTypes.HTML_BLOCK -> BlockKind.HTML
            MarkdownTokenTypes.HORIZONTAL_RULE -> BlockKind.RULE
            MarkdownElementTypes.LINK_DEFINITION -> BlockKind.LINK_DEFINITION
            MarkdownTokenTypes.EOL, MarkdownTokenTypes.WHITE_SPACE -> BlockKind.BLANK
            else -> BlockKind.OTHER
        }

        internal val HEADING_LEVELS: Map<IElementType, Int> = mapOf(
            MarkdownElementTypes.ATX_1 to 1,
            MarkdownElementTypes.ATX_2 to 2,
            MarkdownElementTypes.ATX_3 to 3,
            MarkdownElementTypes.ATX_4 to 4,
            MarkdownElementTypes.ATX_5 to 5,
            MarkdownElementTypes.ATX_6 to 6,
            MarkdownElementTypes.SETEXT_1 to 1,
            MarkdownElementTypes.SETEXT_2 to 2
        )
    }
}

/** Walks the syntax tree once and collects everything the live editor styles. */
private class SyntaxCollector(private val source: String) {
    val inlineSpans = mutableListOf<InlineSpan>()
    val headings = mutableListOf<HeadingLine>()
    val tasks = mutableListOf<TaskMarker>()
    val bullets = mutableListOf<SourceRange>()
    val monospaceBlocks = mutableListOf<SourceRange>()

    fun index() = MarkdownSyntaxIndex(inlineSpans, headings, tasks, bullets, monospaceBlocks)

    fun visit(node: ASTNode) {
        collect(node)
        if (node.type !in OPAQUE) node.children.forEach(::visit)
    }

    private fun collect(node: ASTNode) {
        val range = SourceRange(node.startOffset, node.endOffset)
        when (node.type) {
            MarkdownElementTypes.STRONG, MarkdownElementTypes.EMPH -> {
                val kind = if (node.type ==
                    MarkdownElementTypes.STRONG
                ) {
                    InlineKind.STRONG
                } else {
                    InlineKind.EMPHASIS
                }
                inlineSpans += InlineSpan(kind, range, node.childRanges(MarkdownTokenTypes.EMPH))
            }

            GFMElementTypes.STRIKETHROUGH ->
                inlineSpans +=
                    InlineSpan(
                        InlineKind.STRIKETHROUGH,
                        range,
                        node.childRanges(GFMTokenTypes.TILDE)
                    )

            MarkdownElementTypes.CODE_SPAN -> inlineSpans += InlineSpan(
                InlineKind.CODE,
                range,
                node.childRanges(MarkdownTokenTypes.BACKTICK, MarkdownTokenTypes.ESCAPED_BACKTICKS)
            )

            MarkdownElementTypes.INLINE_LINK -> linkSpan(node, range)?.let { inlineSpans += it }

            in MarkdownDocument.HEADING_LEVELS -> headingLine(node, range)?.let { headings += it }

            GFMTokenTypes.CHECK_BOX -> taskMarker(range)?.let { tasks += it }

            MarkdownTokenTypes.LIST_BULLET -> bullets += range

            in OPAQUE -> monospaceBlocks += range
        }
    }

    private fun linkSpan(node: ASTNode, range: SourceRange): InlineSpan? {
        val text =
            node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT } ?: return null
        val markers = listOf(
            SourceRange(text.startOffset, text.startOffset + 1),
            SourceRange(text.endOffset - 1, node.endOffset)
        )
        return InlineSpan(InlineKind.LINK, range, markers)
    }

    private fun headingLine(node: ASTNode, range: SourceRange): HeadingLine? {
        val level = MarkdownDocument.HEADING_LEVELS.getValue(node.type)
        val marker = node.children.firstOrNull {
            it.type == MarkdownTokenTypes.ATX_HEADER ||
                it.type == MarkdownTokenTypes.SETEXT_1 ||
                it.type == MarkdownTokenTypes.SETEXT_2
        } ?: return null
        // Hide the space after the hashes together with them.
        val end = if (source.getOrNull(marker.endOffset) ==
            ' '
        ) {
            marker.endOffset + 1
        } else {
            marker.endOffset
        }
        return HeadingLine(level, range, SourceRange(marker.startOffset, end))
    }

    private fun taskMarker(range: SourceRange): TaskMarker? {
        val open = source.indexOf('[', range.start)
        if (open < 0 || open + CHECKBOX_LENGTH > range.end) return null
        val checked = source[open + 1].lowercaseChar() == 'x'
        return TaskMarker(SourceRange(open, open + CHECKBOX_LENGTH), checked)
    }

    /** Ranges of the children of [types], merging adjacent ones (`**` comes as two tokens). */
    private fun ASTNode.childRanges(vararg types: IElementType): List<SourceRange> =
        children.filter { it.type in types }
            .map { SourceRange(it.startOffset, it.endOffset) }
            .fold(mutableListOf()) { merged, range ->
                val last = merged.lastOrNull()
                if (last != null && last.end == range.start) {
                    merged[merged.lastIndex] = SourceRange(last.start, range.end)
                } else {
                    merged += range
                }
                merged
            }

    companion object {
        private const val CHECKBOX_LENGTH = 3

        /** Blocks shown verbatim in a monospace style and never parsed further. */
        val OPAQUE: Set<IElementType> = setOf(
            MarkdownElementTypes.CODE_FENCE,
            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.HTML_BLOCK,
            GFMElementTypes.TABLE
        )
    }
}
