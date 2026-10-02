// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** Column alignment from the delimiter row of a GFM table. */
enum class TableAlignment(val delimiter: String) {
    NONE("---"),
    LEFT(":---"),
    CENTER(":---:"),
    RIGHT("---:")
}

/**
 * A GFM table for the grid editor. Cells keep their markdown source. While no edit is made,
 * [serialize] returns the original text unchanged; after an edit the table is written in a
 * clean `| a | b |` form that keeps the alignments, line endings and trailing newline.
 */
class MarkdownTable private constructor(
    val header: List<String>,
    val alignments: List<TableAlignment>,
    val rows: List<List<String>>,
    private val format: Format,
    private val original: String?
) {
    /** Line ending and trailing text of the original table, kept when it is rewritten. */
    private data class Format(val lineEnding: String, val trailing: String)

    val columnCount: Int get() = header.size

    /** Replaces the cell at [row] (or the header when [row] is [HEADER_ROW]) and [column]. */
    fun withCell(row: Int, column: Int, text: String): MarkdownTable {
        val cell = escapeCell(text)
        return if (row == HEADER_ROW) {
            edited(header = header.replaced(column, cell))
        } else {
            edited(rows = rows.replaced(row, rows[row].replaced(column, cell)))
        }
    }

    fun withRowAdded(at: Int = rows.size): MarkdownTable =
        edited(rows = rows.toMutableList().apply { add(at, List(columnCount) { "" }) })

    fun withRowRemoved(at: Int): MarkdownTable = edited(
        rows = rows.filterIndexed { index, _ ->
            index !=
                at
        }
    )

    fun withColumnAdded(at: Int = columnCount): MarkdownTable = edited(
        header = header.inserted(at, ""),
        alignments = alignments.inserted(at, TableAlignment.NONE),
        rows = rows.map { it.inserted(at, "") }
    )

    fun withColumnRemoved(at: Int): MarkdownTable {
        require(columnCount > 1) { "A table needs at least one column" }
        return edited(
            header = header.removedAt(at),
            alignments = alignments.removedAt(at),
            rows = rows.map { it.removedAt(at) }
        )
    }

    fun serialize(): String = original ?: buildString {
        val lines = listOf(header, alignments.map { it.delimiter }) + rows
        append(
            lines.joinToString(format.lineEnding) { cells ->
                cells.joinToString(" | ", "| ", " |")
            }
        )
        append(format.trailing)
    }

    private fun edited(
        header: List<String> = this.header,
        alignments: List<TableAlignment> = this.alignments,
        rows: List<List<String>> = this.rows
    ) = MarkdownTable(header, alignments, rows, format, original = null)

    companion object {
        const val HEADER_ROW = -1

        fun parse(text: String): MarkdownTable {
            val body = text.trimEnd('\n', '\r')
            val lineEnding = if (body.contains("\r\n")) "\r\n" else "\n"
            val lines = body.split(lineEnding)
            require(lines.size >= 2) { "A table needs a header and a delimiter row" }
            val cellRows = lines.map(::splitCells)
            val columns = cellRows.maxOf { it.size }
            val alignments = cellRows[1].map(::alignment).padded(columns, TableAlignment.NONE)
            return MarkdownTable(
                header = cellRows[0].padded(columns, ""),
                alignments = alignments,
                rows = cellRows.drop(2).map { it.padded(columns, "") },
                format = Format(lineEnding, text.substring(body.length)),
                original = text
            )
        }

        /** Splits a table row on pipes that are not escaped, dropping the outer ones. */
        private fun splitCells(line: String): List<String> {
            val cells = mutableListOf<String>()
            val cell = StringBuilder()
            var escaped = false
            line.trim().forEach { char ->
                when {
                    escaped -> {
                        cell.append(char)
                        escaped = false
                    }

                    char == '\\' -> {
                        cell.append(char)
                        escaped = true
                    }

                    char == '|' -> {
                        cells += cell.toString()
                        cell.clear()
                    }

                    else -> cell.append(char)
                }
            }
            cells += cell.toString()
            val trimmed = line.trim()
            if (trimmed.startsWith("|")) cells.removeAt(0)
            if (trimmed.endsWith("|") && !trimmed.endsWith("\\|") &&
                cells.isNotEmpty()
            ) {
                cells.removeAt(cells.lastIndex)
            }
            return cells.map { it.trim() }
        }

        private fun alignment(cell: String): TableAlignment {
            val left = cell.startsWith(":")
            val right = cell.endsWith(":")
            return when {
                left && right -> TableAlignment.CENTER
                left -> TableAlignment.LEFT
                right -> TableAlignment.RIGHT
                else -> TableAlignment.NONE
            }
        }

        /** Cells are single-line and pipes must be escaped to stay inside the cell. */
        private fun escapeCell(text: String): String {
            val singleLine = text.replace("\r\n", " ").replace('\n', ' ')
            return buildString {
                singleLine.forEachIndexed { index, char ->
                    if (char == '|' && (index == 0 || singleLine[index - 1] != '\\')) append('\\')
                    append(char)
                }
            }
        }

        private fun <T> List<T>.padded(size: Int, filler: T): List<T> =
            this + List(size - this.size) { filler }

        private fun <T> List<T>.replaced(index: Int, value: T): List<T> =
            toMutableList().apply { set(index, value) }

        private fun <T> List<T>.inserted(index: Int, value: T): List<T> =
            toMutableList().apply { add(index, value) }

        private fun <T> List<T>.removedAt(index: Int): List<T> = filterIndexed { i, _ ->
            i != index
        }
    }
}
