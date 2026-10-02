// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CodeBlockTest {
    private fun codeSegments(name: String): List<String> {
        val source = MarkdownCorpus.read(name)
        return DocumentSegments.split(MarkdownDocument.parse(source))
            .filter { it.kind == SegmentKind.CODE }
            .map { source.substring(it.range.start, it.range.end) }
    }

    @Test
    fun `unedited code blocks of the corpus serialize to the original`() {
        val blocks = codeSegments("04-code.md") + codeSegments("12-deck-description.md")

        assertEquals(4, blocks.size)
        blocks.forEach { assertEquals(it, CodeBlock.parse(it).serialize()) }
    }

    @Test
    fun `exposes the language and the content without fences`() {
        val block = CodeBlock.parse("```kotlin\nfun main() {}\n```\n")

        assertEquals("kotlin", block.language)
        assertEquals("fun main() {}", block.content)
    }

    @Test
    fun `keeps the fence and language when the content is edited`() {
        val edited = CodeBlock.parse("~~~~ json\n{}\n~~~~\n").withContent("{\"a\": 1}\n{}")

        assertEquals("~~~~ json\n{\"a\": 1}\n{}\n~~~~\n", edited.serialize())
    }

    @Test
    fun `closes an unclosed fence when edited`() {
        val edited = CodeBlock.parse("```\nopen").withContent("changed")

        assertEquals("```\nchanged\n```", edited.serialize())
    }

    @Test
    fun `edits indented blocks keeping their indentation`() {
        val block = CodeBlock.parse("    one\n    two\n")

        assertEquals("one\ntwo", block.content)
        assertEquals("    one\n\n    three\n", block.withContent("one\n\nthree").serialize())
    }

    @Test
    fun `keeps Windows line endings`() {
        val edited = CodeBlock.parse("```\r\na\r\n```\r\n").withContent("b")

        assertEquals("```\r\nb\r\n```\r\n", edited.serialize())
    }
}
