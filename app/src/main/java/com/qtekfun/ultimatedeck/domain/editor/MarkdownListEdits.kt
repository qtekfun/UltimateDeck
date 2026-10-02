// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** List commands as minimal changes on the markdown source, like [MarkdownEdits]. */
object MarkdownListEdits {
    private const val BULLET = "- "
    private const val CHECKBOX = "[ ] "
    private val listItem =
        Regex("""^(?<indent>\s*)(?<bullet>[-*+])(?<space>\s+)(?<checkbox>\[[ xX]]\s+)?""")
    private val orderedItem =
        Regex("""^(?<indent>\s*)(?<number>\d{1,9})(?<delimiter>[.)])(?<space>\s+)""")

    /** Turns the selected lines into a bullet list, or back into plain lines if they all are one. */
    fun toggleBulletList(source: String, selection: SourceRange): EditResult =
        editLines(source, selection) { lines ->
            val allBullets = lines.filter { it.isNotBlank() }.all { listItem.containsMatchIn(it) }
            lines.map { line ->
                val match = listItem.find(line)
                when {
                    line.isBlank() -> line

                    allBullets && match != null ->
                        match.groups["indent"]!!.value + line.substring(match.range.last + 1)

                    match != null -> line

                    else -> insertAfterIndent(line, BULLET)
                }
            }
        }

    /** Turns the selected lines into a task list, or removes their checkboxes if they all have one. */
    fun toggleTaskList(source: String, selection: SourceRange): EditResult =
        editLines(source, selection) { lines ->
            val allTasks = lines.filter { it.isNotBlank() }
                .all { listItem.find(it)?.groups?.get("checkbox") != null }
            lines.map { line ->
                val match = listItem.find(line)
                when {
                    line.isBlank() -> line

                    allTasks && match != null -> line.removeRange(match.groups["checkbox"]!!.range)

                    match != null && match.groups["checkbox"] == null -> {
                        val afterBullet = match.groups["space"]!!.range.last + 1
                        line.substring(0, afterBullet) + CHECKBOX + line.substring(afterBullet)
                    }

                    match != null -> line

                    else -> insertAfterIndent(line, BULLET + CHECKBOX)
                }
            }
        }

    /**
     * Enter inside a list: continues it with a new item (unchecked for tasks, next number for
     * ordered lists), or ends it when the current item is empty. Returns null outside lists so
     * the caller inserts a plain line break.
     */
    fun smartEnter(source: String, cursor: Int): EditResult? {
        val lineStart = source.lastIndexOf('\n', cursor - 1) + 1
        val lineEnd = source.indexOf('\n', cursor).let { if (it < 0) source.length else it }
        val line = source.substring(lineStart, lineEnd).removeSuffix("\r")
        val prefix = listItem.find(line)?.value ?: orderedItem.find(line)?.value ?: return null
        return if (line.substring(prefix.length).isBlank()) {
            EditResult(
                source.removeRange(lineStart, lineStart + line.length),
                SourceRange(lineStart, lineStart)
            )
        } else {
            val lineEnding = if (source.startsWith("\r\n", lineEnd - 1)) "\r\n" else "\n"
            val insertion = lineEnding + nextPrefix(line)
            val caret = cursor + insertion.length
            EditResult(
                source.substring(0, cursor) + insertion + source.substring(cursor),
                SourceRange(caret, caret)
            )
        }
    }

    private fun nextPrefix(line: String): String {
        val bullet = listItem.find(line)
        if (bullet != null) {
            val task = if (bullet.groups["checkbox"] != null) CHECKBOX else ""
            return bullet.groups["indent"]!!.value + bullet.groups["bullet"]!!.value + " " + task
        }
        val ordered = orderedItem.find(line)!!
        val number = ordered.groups["number"]!!.value.toLong() + 1
        return ordered.groups["indent"]!!.value + number + ordered.groups["delimiter"]!!.value + " "
    }

    private fun insertAfterIndent(line: String, prefix: String): String {
        val indent = line.length - line.trimStart().length
        return line.substring(0, indent) + prefix + line.substring(indent)
    }
}
