// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import com.qtekfun.ultimatedeck.domain.editor.MarkdownTable.Companion.HEADER_ROW
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MarkdownTableTest {
    private val corpusTable = MarkdownCorpus.read("05-tables.md")

    @Test
    fun `parses cells, alignments and escaped pipes`() {
        val table = MarkdownTable.parse(corpusTable)

        assertEquals(listOf("Column", "Left", "Right"), table.header)
        assertEquals(
            listOf(TableAlignment.NONE, TableAlignment.LEFT, TableAlignment.RIGHT),
            table.alignments
        )
        assertEquals(listOf(listOf("a", "b", "c"), listOf("**x**", "`y`", "z\\|z")), table.rows)
    }

    @Test
    fun `an unedited table serializes to the original text`() {
        assertEquals(corpusTable, MarkdownTable.parse(corpusTable).serialize())
        val deck = MarkdownCorpus.read("12-deck-description.md")
        val segment = DocumentSegments.split(MarkdownDocument.parse(deck)).first {
            it.kind ==
                SegmentKind.TABLE
        }
        val text = deck.substring(segment.range.start, segment.range.end)
        assertEquals(text, MarkdownTable.parse(text).serialize())
    }

    @Test
    fun `editing a cell rewrites the table keeping alignments and escapes`() {
        val edited = MarkdownTable.parse(corpusTable).withCell(0, 1, "new")

        assertEquals(
            "| Column | Left | Right |\n| --- | :--- | ---: |\n| a | new | c |\n| **x** | `y` | z\\|z |\n",
            edited.serialize()
        )
    }

    @Test
    fun `editing the header works too`() {
        val edited = MarkdownTable.parse("| a |\n|---|\n").withCell(HEADER_ROW, 0, "Title")

        assertEquals("| Title |\n| --- |\n", edited.serialize())
    }

    @Test
    fun `escapes pipes and flattens new lines typed in a cell`() {
        val edited = MarkdownTable.parse("| a |\n|---|\n| b |").withCell(0, 0, "x|y\nz")

        assertEquals("| a |\n| --- |\n| x\\|y z |", edited.serialize())
    }

    @Test
    fun `adds and removes rows`() {
        val table = MarkdownTable.parse("| a | b |\n|---|:-:|\n| 1 | 2 |\n")

        assertEquals(
            "| a | b |\n| --- | :---: |\n| 1 | 2 |\n|  |  |\n",
            table.withRowAdded().serialize()
        )
        assertEquals(
            "| a | b |\n| --- | :---: |\n|  |  |\n| 1 | 2 |\n",
            table.withRowAdded(0).serialize()
        )
        assertEquals("| a | b |\n| --- | :---: |\n", table.withRowRemoved(0).serialize())
    }

    @Test
    fun `adds and removes columns with their alignment`() {
        val table = MarkdownTable.parse("| a | b |\n|---|--:|\n| 1 | 2 |\n")

        assertEquals(
            "| a | b |  |\n| --- | ---: | --- |\n| 1 | 2 |  |\n",
            table.withColumnAdded().serialize()
        )
        assertEquals("| b |\n| ---: |\n| 2 |\n", table.withColumnRemoved(0).serialize())
    }

    @Test
    fun `refuses to remove the last column`() {
        assertThrows<IllegalArgumentException> {
            MarkdownTable.parse("| a |\n|---|\n").withColumnRemoved(0)
        }
    }

    @Test
    fun `keeps Windows line endings when rewriting`() {
        val edited = MarkdownTable.parse("| a |\r\n|---|\r\n| b |\r\n").withCell(0, 0, "c")

        assertEquals("| a |\r\n| --- |\r\n| c |\r\n", edited.serialize())
    }

    @Test
    fun `pads short rows to the widest row`() {
        val table = MarkdownTable.parse("| a | b |\n|---|---|\n| only |\n")

        assertEquals(listOf("only", ""), table.rows.single())
    }

    @Test
    fun `parses rows without outer pipes`() {
        val table = MarkdownTable.parse("a | b\n--- | ---\n1 | 2")

        assertEquals(listOf("a", "b"), table.header)
        assertEquals(listOf(listOf("1", "2")), table.rows)
    }

    @Test
    fun `rejects text that is not a table`() {
        assertThrows<IllegalArgumentException> { MarkdownTable.parse("| only a header |") }
    }
}
