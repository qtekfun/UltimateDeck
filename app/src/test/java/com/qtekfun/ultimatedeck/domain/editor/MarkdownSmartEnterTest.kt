// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MarkdownSmartEnterTest {

    private fun enter(source: String, cursor: Int = source.length) =
        MarkdownListEdits.smartEnter(source, cursor)

    @Test
    fun `continues a bullet list with the same bullet`() {
        val result = enter("* one")

        assertEquals("* one\n* ", result?.text)
        assertEquals(SourceRange(8, 8), result?.selection)
    }

    @Test
    fun `continues a checklist with an unchecked task`() {
        assertEquals("- [x] done\n- [ ] ", enter("- [x] done")?.text)
    }

    @Test
    fun `keeps the indentation of nested items`() {
        assertEquals("- a\n  - [ ] b\n  - [ ] ", enter("- a\n  - [ ] b")?.text)
    }

    @Test
    fun `continues ordered lists with the next number`() {
        assertEquals("9) nine\n10) ", enter("9) nine")?.text)
    }

    @Test
    fun `splits an item when the cursor is in the middle`() {
        assertEquals("- ab\n- cd", enter("- abcd", cursor = 4)?.text)
    }

    @Test
    fun `ends the list on an empty item`() {
        val result = enter("- [ ] task\n- [ ] ")

        assertEquals("- [ ] task\n", result?.text)
        assertEquals(SourceRange(11, 11), result?.selection)
    }

    @Test
    fun `keeps Windows line endings`() {
        assertEquals("- a\r\n- \r\n- b", enter("- a\r\n- b", cursor = 3)?.text)
    }

    @Test
    fun `returns null outside lists`() {
        assertNull(enter("plain text"))
    }

    @Test
    fun `turns a line into a heading and back`() {
        val heading = MarkdownEdits.toggleHeading("intro\ntitle", SourceRange(8, 8))
        assertEquals("intro\n## title", heading.text)

        val plain = MarkdownEdits.toggleHeading(heading.text, SourceRange(10, 10))
        assertEquals("intro\ntitle", plain.text)
    }
}
