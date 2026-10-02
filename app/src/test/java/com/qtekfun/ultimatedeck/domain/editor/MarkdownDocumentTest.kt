// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MarkdownDocumentTest {

    @ParameterizedTest
    @MethodSource("corpus")
    fun `round trip returns the source unchanged`(name: String) {
        val source = MarkdownCorpus.read(name)

        assertEquals(source, MarkdownDocument.parse(source).toMarkdown())
    }

    @ParameterizedTest
    @MethodSource("corpus")
    fun `blocks are contiguous and cover the whole source`(name: String) {
        val source = MarkdownCorpus.read(name)
        val blocks = MarkdownDocument.parse(source).blocks

        var cursor = 0
        blocks.forEach { block ->
            assertEquals(cursor, block.range.start, "gap or overlap before $block")
            cursor = block.range.end
        }
        assertEquals(source.length, cursor)
    }

    @ParameterizedTest
    @MethodSource("corpus")
    fun `every syntax range lies inside the source`(name: String) {
        val source = MarkdownCorpus.read(name)
        val document = MarkdownDocument.parse(source)
        val ranges = document.inlineSpans.flatMap { it.markers + it.range } +
            document.headings.flatMap { listOf(it.range, it.marker) } +
            document.tasks.map { it.range } +
            document.bullets +
            document.monospaceBlocks

        ranges.forEach { assertTrue(it.end <= source.length, "$it out of $name") }
    }

    @Test
    fun `an empty document has no blocks`() {
        val document = MarkdownDocument.parse("")

        assertTrue(document.blocks.isEmpty())
        assertEquals("", document.toMarkdown())
    }

    @Test
    fun `finds task checkboxes, nested ones included, with their state`() {
        val source = MarkdownCorpus.read("01-task-lists.md")
        val tasks = MarkdownDocument.parse(source).tasks

        assertEquals(listOf(false, true, false, true, false), tasks.map { it.checked })
        tasks.forEach { task ->
            assertTrue(source.substring(task.range.start, task.range.end) in setOf("[ ]", "[x]"))
        }
    }

    @Test
    fun `finds checkboxes in a document with Windows line endings`() {
        val tasks = MarkdownDocument.parse(MarkdownCorpus.read("13-crlf.md")).tasks

        assertEquals(listOf(false, true), tasks.map { it.checked })
    }

    @Test
    fun `finds heading levels and their markers`() {
        val source = MarkdownCorpus.read("06-headings.md")
        val headings = MarkdownDocument.parse(source).headings

        assertEquals(listOf(1, 2, 6, 1, 2), headings.map { it.level })
        val first = headings.first().marker
        assertEquals("# ", source.substring(first.start, first.end))
    }

    @Test
    fun `finds strong, emphasis and strikethrough spans with their markers`() {
        val source = MarkdownCorpus.read("08-emphasis.md")
        val spans = MarkdownDocument.parse(source).inlineSpans

        val kinds = spans.map { it.kind }.toSet()
        assertTrue(
            kinds.containsAll(
                setOf(InlineKind.STRONG, InlineKind.EMPHASIS, InlineKind.STRIKETHROUGH)
            )
        )
        val strong = spans.first { it.kind == InlineKind.STRONG }
        assertEquals(listOf("**", "**"), strong.markers.map { source.substring(it.start, it.end) })
    }

    @Test
    fun `treats code blocks as opaque so their content is not styled`() {
        val source = MarkdownCorpus.read("04-code.md")
        val document = MarkdownDocument.parse(source)

        assertEquals(3, document.monospaceBlocks.size)
        val fence = document.monospaceBlocks.first {
            source.substring(it.start, it.end).startsWith("```")
        }
        assertFalse(
            document.inlineSpans.any {
                it.kind == InlineKind.STRONG &&
                    it.range.start in fence
            }
        )
        assertTrue(document.inlineSpans.any { it.kind == InlineKind.CODE })
    }

    @Test
    fun `treats tables as opaque blocks`() {
        val document = MarkdownDocument.parse(MarkdownCorpus.read("05-tables.md"))

        assertTrue(document.blocks.any { it.kind == BlockKind.TABLE })
        assertEquals(1, document.monospaceBlocks.size)
    }

    @Test
    fun `marks the brackets and destination of inline links`() {
        val source = MarkdownCorpus.read("03-links-images.md")
        val link = MarkdownDocument.parse(source).inlineSpans.first { it.kind == InlineKind.LINK }

        val markers = link.markers.map { source.substring(it.start, it.end) }
        assertEquals("[", markers.first())
        assertEquals("](https://nextcloud.com \"Nextcloud home\")", markers.last())
    }

    @Test
    fun `finds list bullets`() {
        val document = MarkdownDocument.parse(MarkdownCorpus.read("02-lists.md"))

        assertTrue(document.bullets.isNotEmpty())
        assertTrue(document.blocks.any { it.kind == BlockKind.ORDERED_LIST })
    }

    companion object {
        @JvmStatic
        fun corpus(): List<String> = MarkdownCorpus.names
    }
}
