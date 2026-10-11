// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DocumentSegmentsTest {

    private fun segmentsOf(source: String) = DocumentSegments.split(MarkdownDocument.parse(source))

    @ParameterizedTest
    @MethodSource("corpus")
    fun `segments are contiguous and rebuild the source`(name: String) {
        val source = MarkdownCorpus.read(name)
        val segments = segmentsOf(source)

        assertEquals(
            source,
            segments.joinToString("") {
                source.substring(it.range.start, it.range.end)
            }
        )
        segments.zipWithNext().forEach { (a, b) -> assertEquals(a.range.end, b.range.start) }
    }

    @Test
    fun `merges text blocks and isolates tables and code`() {
        val source = "# Title\n\nText\n- [ ] task\n\n" +
            "| a | b |\n|---|---|\n| 1 | 2 |\n\n" +
            "```\ncode\n```\n\nAfter\n"

        val kinds = segmentsOf(source).map { it.kind }

        assertEquals(
            listOf(
                SegmentKind.TEXT,
                SegmentKind.TABLE,
                SegmentKind.TEXT,
                SegmentKind.CODE,
                SegmentKind.TEXT
            ),
            kinds
        )
    }

    @Test
    fun `keeps html blocks verbatim`() {
        val kinds = segmentsOf(MarkdownCorpus.read("09-html.md")).map { it.kind }

        assertEquals(true, SegmentKind.RAW in kinds)
    }

    @Test
    fun `replacing a segment only changes its range`() {
        val source = MarkdownCorpus.read("12-deck-description.md")
        val table = segmentsOf(source).first { it.kind == SegmentKind.TABLE }

        val edited = DocumentSegments.replace(source, table, "| new |\n|---|\n")

        assertEquals(source.substring(0, table.range.start), edited.substring(0, table.range.start))
        assertEquals(
            source.substring(table.range.end),
            edited.substring(edited.length - (source.length - table.range.end))
        )
    }

    companion object {
        @JvmStatic
        fun corpus(): List<String> = MarkdownCorpus.names
    }

    @Test
    fun `deleting a table segment removes it with its line break and keeps the rest`() {
        val before = "Intro\n\n"
        val after = "\nAfter\n"
        val source = before + "| a | b |\n|---|---|\n| 1 | 2 |\n" + after
        val table = segmentsOf(source).single { it.kind == SegmentKind.TABLE }

        val result = DocumentSegments.remove(source, table)

        assertEquals(before + "\nAfter\n", result)
        assertEquals(
            emptyList<SegmentKind>(),
            segmentsOf(result).map { it.kind }.filter { it == SegmentKind.TABLE }
        )
    }

    @Test
    fun `removing a table at the end of the source without a final line break`() {
        val source = "Intro\n\n| a |\n|---|\n| 1 |"
        val table = segmentsOf(source).single { it.kind == SegmentKind.TABLE }

        assertEquals("Intro\n\n", DocumentSegments.remove(source, table))
    }
}
