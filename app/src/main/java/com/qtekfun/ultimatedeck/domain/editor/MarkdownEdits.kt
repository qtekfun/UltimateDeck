// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** Text after an edit and the selection to show, both on the markdown source. */
data class EditResult(val text: String, val selection: SourceRange)

/**
 * Formatting commands as minimal changes on the markdown source: only the syntax characters
 * involved are inserted or removed, the rest of the text is left byte for byte as it was.
 * List commands live in [MarkdownListEdits].
 */
object MarkdownEdits {
    private const val BOLD = "**"
    private const val ITALIC = "_"
    private const val HEADING = "## "
    private val heading = Regex("""^#{1,6}[ \t]+""")

    /** Ticks or unticks [task], changing exactly one character. */
    fun toggleTask(source: String, task: TaskMarker): String {
        val inner = task.range.start + 1
        val replacement = if (task.checked) ' ' else 'x'
        return source.substring(0, inner) + replacement + source.substring(inner + 1)
    }

    fun toggleBold(source: String, selection: SourceRange): EditResult =
        toggleWrap(source, selection, BOLD)

    fun toggleItalic(source: String, selection: SourceRange): EditResult =
        toggleWrap(source, selection, ITALIC)

    /**
     * Wraps the selection in [marker], or unwraps it if the markers are right outside or at the
     * edges of the selection. An empty selection gets an empty pair with the cursor inside.
     */
    fun toggleWrap(source: String, selection: SourceRange, marker: String): EditResult {
        val (start, end) = selection
        val size = marker.length
        val outside = start >= size &&
            source.startsWith(marker, start - size) &&
            source.startsWith(marker, end)
        val inside = selection.length >= 2 * size &&
            source.startsWith(marker, start) &&
            source.startsWith(marker, end - size)
        return when {
            outside -> EditResult(
                source.removeRange(end, end + size).removeRange(start - size, start),
                SourceRange(start - size, end - size)
            )

            inside -> EditResult(
                source.removeRange(end - size, end).removeRange(start, start + size),
                SourceRange(start, end - 2 * size)
            )

            else -> EditResult(
                source.substring(0, start) + marker + source.substring(start, end) + marker +
                    source.substring(end),
                SourceRange(start + size, end + size)
            )
        }
    }

    /** Turns the selected lines into headings, or back into plain text if they are ones. */
    fun toggleHeading(source: String, selection: SourceRange): EditResult =
        editLines(source, selection) { lines ->
            lines.map { line ->
                val match = heading.find(line)
                when {
                    match != null -> line.substring(match.range.last + 1)
                    line.isBlank() -> line
                    else -> HEADING + line
                }
            }
        }
}

/** Applies [transform] to the whole lines touched by [selection] and selects the result. */
internal fun editLines(
    source: String,
    selection: SourceRange,
    transform: (List<String>) -> List<String>
): EditResult {
    val first = source.lastIndexOf('\n', selection.start - 1) + 1
    val lastBreak = source.indexOf('\n', maxOf(selection.end - 1, selection.start))
    val last = if (lastBreak < 0) source.length else lastBreak
    val edited = transform(source.substring(first, last).split('\n')).joinToString("\n")
    return EditResult(
        source.substring(0, first) + edited + source.substring(last),
        SourceRange(first, first + edited.length)
    )
}
