// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

/** Markdown samples in src/test/resources/markdown-corpus, read byte for byte. */
object MarkdownCorpus {
    private const val DIRECTORY = "markdown-corpus"

    val names: List<String> = listOf(
        "01-task-lists.md",
        "02-lists.md",
        "03-links-images.md",
        "04-code.md",
        "05-tables.md",
        "06-headings.md",
        "07-quotes.md",
        "08-emphasis.md",
        "09-html.md",
        "10-breaks-whitespace.md",
        "11-unicode.md",
        "12-deck-description.md",
        "13-crlf.md",
        "14-no-final-newline.md"
    )

    fun read(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("$DIRECTORY/$name")) {
            "Missing corpus file $name"
        }
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
