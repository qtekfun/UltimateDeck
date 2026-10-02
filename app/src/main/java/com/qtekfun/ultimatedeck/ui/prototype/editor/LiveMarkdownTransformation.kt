// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import com.qtekfun.ultimatedeck.domain.editor.SourceRange

private const val UNCHECKED_BOX = '☐'
private const val CHECKED_BOX = '☑'
private const val BULLET = '•'

/**
 * Displays markdown rendered: the text stays the markdown source, but styles are applied and
 * the syntax markers are hidden. With [revealMarkers] the markers of the span holding the
 * cursor are shown, like Obsidian; without it they never are, like a rich text editor.
 * Task checkboxes are drawn as ballot boxes and list bullets as dots.
 */
internal class LiveMarkdownTransformation(
    private val document: MarkdownDocument,
    private val selection: TextRange,
    private val styles: LiveMarkdownStyles,
    private val revealMarkers: Boolean = false
) : VisualTransformation {

    /** Mapping of the last [filter] call, used to hit-test checkboxes. */
    var mapping: OffsetMapping = OffsetMapping.Identity
        private set

    override fun filter(text: AnnotatedString): TransformedText {
        val doc = if (text.text == document.source) document else MarkdownDocument.parse(text.text)
        val hidden = mutableListOf<SourceRange>()
        val replacements = mutableMapOf<Int, Char>()
        val styled = buildAnnotatedString {
            append(doc.source)
            doc.monospaceBlocks.forEach { addStyle(styles.monospaceBlock, it.start, it.end) }
            doc.headings.forEach { heading ->
                addStyle(styles.heading(heading.level), heading.range.start, heading.range.end)
                addStyle(styles.marker, heading.marker.start, heading.marker.end)
                if (!touchesSelection(heading.range)) hidden += heading.marker
            }
            doc.inlineSpans.forEach { span ->
                addStyle(styles.inline(span.kind), span.range.start, span.range.end)
                span.markers.forEach { addStyle(styles.marker, it.start, it.end) }
                if (!touchesSelection(span.range)) hidden += span.markers
            }
            doc.tasks.forEach { task ->
                val inner = task.range.start + 1
                if (!touchesSelection(task.range)) {
                    hidden += SourceRange(task.range.start, inner)
                    hidden += SourceRange(task.range.end - 1, task.range.end)
                    replacements[inner] = if (task.checked) CHECKED_BOX else UNCHECKED_BOX
                }
                addStyle(styles.checkbox, inner, inner + 1)
            }
            doc.bullets.forEach { bullet ->
                val index = (bullet.start until bullet.end).firstOrNull {
                    !doc.source[it].isWhitespace()
                }
                if (index != null) {
                    replacements[index] = BULLET
                    addStyle(styles.marker, index, index + 1)
                }
            }
        }
        val merged = mergeRanges(hidden)
        mapping = HiddenRangesOffsetMapping(merged, doc.source.length)
        return TransformedText(
            withoutHidden(withReplacements(styled, replacements), merged),
            mapping
        )
    }

    private fun touchesSelection(range: SourceRange): Boolean =
        revealMarkers && selection.min <= range.end && selection.max >= range.start

    private fun withReplacements(
        text: AnnotatedString,
        replacements: Map<Int, Char>
    ): AnnotatedString {
        if (replacements.isEmpty()) return text
        val chars = text.text.toCharArray()
        replacements.forEach { (index, char) -> chars[index] = char }
        return AnnotatedString(String(chars), text.spanStyles, text.paragraphStyles)
    }

    private fun withoutHidden(text: AnnotatedString, hidden: List<SourceRange>): AnnotatedString {
        if (hidden.isEmpty()) return text
        return buildAnnotatedString {
            var cursor = 0
            hidden.forEach { range ->
                append(text.subSequence(cursor, range.start))
                cursor = range.end
            }
            append(text.subSequence(cursor, text.length))
        }
    }
}
