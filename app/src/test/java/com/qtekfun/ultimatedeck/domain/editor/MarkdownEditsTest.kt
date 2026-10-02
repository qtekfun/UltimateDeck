// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MarkdownEditsTest {

    @Nested
    inner class ToggleTask {
        private val source = MarkdownCorpus.read("01-task-lists.md")
        private val tasks = MarkdownDocument.parse(source).tasks

        @Test
        fun `changes exactly one character per toggle`() {
            tasks.forEach { task ->
                val edited = MarkdownEdits.toggleTask(source, task)

                assertEquals(source.length, edited.length)
                assertEquals(1, source.indices.count { source[it] != edited[it] })
                assertEquals(if (task.checked) ' ' else 'x', edited[task.range.start + 1])
            }
        }

        @Test
        fun `toggling twice restores the original source`() {
            val task = tasks.first()
            val once = MarkdownEdits.toggleTask(source, task)
            val again = MarkdownEdits.toggleTask(once, MarkdownDocument.parse(once).tasks.first())

            assertEquals(source, again)
        }

        @Test
        fun `unticks an uppercase X`() {
            val source = "- [X] Done\n"
            val task = MarkdownDocument.parse(source).tasks.single()

            assertEquals("- [ ] Done\n", MarkdownEdits.toggleTask(source, task))
        }
    }

    @Nested
    inner class ToggleWrap {
        @Test
        fun `wraps the selection in bold markers`() {
            val result = MarkdownEdits.toggleBold("make this bold", SourceRange(5, 9))

            assertEquals("make **this** bold", result.text)
            assertEquals(SourceRange(7, 11), result.selection)
        }

        @Test
        fun `unwraps when the markers are right outside the selection`() {
            val result = MarkdownEdits.toggleBold("make **this** bold", SourceRange(7, 11))

            assertEquals("make this bold", result.text)
            assertEquals(SourceRange(5, 9), result.selection)
        }

        @Test
        fun `unwraps when the selection includes the markers`() {
            val result = MarkdownEdits.toggleBold("make **this** bold", SourceRange(5, 13))

            assertEquals("make this bold", result.text)
            assertEquals(SourceRange(5, 9), result.selection)
        }

        @Test
        fun `inserts an empty pair with the cursor inside for an empty selection`() {
            val result = MarkdownEdits.toggleItalic("ab", SourceRange(1, 1))

            assertEquals("a__b", result.text)
            assertEquals(SourceRange(2, 2), result.selection)
        }

        @Test
        fun `leaves the rest of the source untouched`() {
            val source = MarkdownCorpus.read("12-deck-description.md")
            val start = source.indexOf("sync")
            val result = MarkdownEdits.toggleItalic(source, SourceRange(start, start + 4))

            assertEquals(
                source,
                result.text.removeRange(start + 5, start + 6).removeRange(
                    start,
                    start + 1
                )
            )
        }
    }

    @Nested
    inner class ToggleBulletList {
        @Test
        fun `adds bullets to plain lines keeping their indentation`() {
            val result = MarkdownListEdits.toggleBulletList("one\n  two\nthree", SourceRange(0, 9))

            assertEquals("- one\n  - two\nthree", result.text)
        }

        @Test
        fun `removes bullets when every selected line has one`() {
            val result = MarkdownListEdits.toggleBulletList("- one\n* two\n", SourceRange(0, 10))

            assertEquals("one\ntwo\n", result.text)
        }

        @Test
        fun `only adds bullets to the lines that miss one`() {
            val result = MarkdownListEdits.toggleBulletList("- one\ntwo", SourceRange(0, 9))

            assertEquals("- one\n- two", result.text)
        }

        @Test
        fun `skips blank lines and keeps Windows line endings`() {
            val result = MarkdownListEdits.toggleBulletList("a\r\n\r\nb\r\n", SourceRange(0, 6))

            assertEquals("- a\r\n\r\n- b\r\n", result.text)
        }

        @Test
        fun `selects the edited lines`() {
            val source = "intro\none\ntwo\noutro"
            val result = MarkdownListEdits.toggleBulletList(source, SourceRange(7, 11))

            assertEquals("intro\n- one\n- two\noutro", result.text)
            assertEquals(SourceRange(6, 17), result.selection)
        }
    }

    @Nested
    inner class ToggleTaskList {
        @Test
        fun `turns plain lines into unchecked tasks`() {
            val result = MarkdownListEdits.toggleTaskList("buy milk", SourceRange(0, 0))

            assertEquals("- [ ] buy milk", result.text)
        }

        @Test
        fun `adds checkboxes to existing bullets`() {
            val result = MarkdownListEdits.toggleTaskList("* first\n- second", SourceRange(0, 15))

            assertEquals("* [ ] first\n- [ ] second", result.text)
        }

        @Test
        fun `removes checkboxes when every line is a task, keeping the bullets`() {
            val result = MarkdownListEdits.toggleTaskList(
                "- [x] done\n- [ ] todo",
                SourceRange(0, 20)
            )

            assertEquals("- done\n- todo", result.text)
        }

        @Test
        fun `keeps existing tasks when converting a mixed selection`() {
            val result = MarkdownListEdits.toggleTaskList("- [x] done\n- todo", SourceRange(0, 17))

            assertEquals("- [x] done\n- [ ] todo", result.text)
        }
    }
}
