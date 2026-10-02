// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/**
 * A fenced or indented code block for the code editor, which only shows [content]. While
 * unedited, [serialize] returns the original text; after an edit the fences (or the
 * indentation), the language, the line endings and the trailing newline are kept.
 */
class CodeBlock private constructor(
    val language: String,
    val content: String,
    private val fence: Fence?,
    private val lineEnding: String,
    private val trailing: String,
    private val original: String?
) {
    /** Opening and closing fence lines; the closing one is missing in an unclosed block. */
    private data class Fence(val opening: String, val closing: String?)

    fun withContent(text: String): CodeBlock =
        CodeBlock(language, text, fence, lineEnding, trailing, original = null)

    fun serialize(): String = original ?: buildString {
        val lines = content.split('\n').map { it.removeSuffix("\r") }
        if (fence == null) {
            append(lines.joinToString(lineEnding) { if (it.isEmpty()) it else INDENT + it })
        } else {
            append(fence.opening).append(lineEnding)
            append(lines.joinToString(lineEnding)).append(lineEnding)
            append(fence.closing ?: fence.opening.trimStart().takeWhile { it == '`' || it == '~' })
        }
        append(trailing)
    }

    companion object {
        private const val INDENT = "    "
        private val fenceStart = Regex("""^\s{0,3}(`{3,}|~{3,})(.*)$""")

        fun parse(text: String): CodeBlock {
            val body = text.trimEnd('\n', '\r')
            val lineEnding = if (body.contains("\r\n")) "\r\n" else "\n"
            val lines = body.split(lineEnding)
            val opening = fenceStart.find(lines.first())
            val trailing = text.substring(body.length)
            if (opening == null) {
                val content = lines.joinToString("\n") {
                    it.removePrefix(INDENT).removePrefix("\t")
                }
                return CodeBlock("", content, null, lineEnding, trailing, text)
            }
            val marker = opening.groupValues[1]
            val last = lines.last()
            val closed =
                lines.size > 1 && last.trim().startsWith(marker) &&
                    last.trim().all { it == marker[0] }
            val contentLines = lines.subList(1, if (closed) lines.size - 1 else lines.size)
            return CodeBlock(
                language = opening.groupValues[2].trim(),
                content = contentLines.joinToString("\n"),
                fence = Fence(lines.first(), if (closed) last else null),
                lineEnding = lineEnding,
                trailing = trailing,
                original = text
            )
        }
    }
}
