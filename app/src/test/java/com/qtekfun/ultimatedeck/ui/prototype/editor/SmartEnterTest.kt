// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class SmartEnterTest {

    @Test
    fun `continues a checklist when a line break is typed at the cursor`() {
        val old = TextFieldValue("- [x] done", TextRange(10))
        val typed = TextFieldValue("- [x] done\n", TextRange(11))

        assertEquals(
            TextFieldValue("- [x] done\n- [ ] ", TextRange(17)),
            applySmartEnter(old, typed)
        )
    }

    @Test
    fun `leaves other edits untouched`() {
        val old = TextFieldValue("- item", TextRange(6))
        val typed = TextFieldValue("- items", TextRange(7))

        assertSame(typed, applySmartEnter(old, typed))
    }

    @Test
    fun `leaves a line break outside lists untouched`() {
        val old = TextFieldValue("plain", TextRange(5))
        val typed = TextFieldValue("plain\n", TextRange(6))

        assertSame(typed, applySmartEnter(old, typed))
    }

    @Test
    fun `syncing keeps the selection inside the new text`() {
        val value = TextFieldValue("long text", TextRange(4, 9))

        assertEquals(TextFieldValue("long", TextRange(4, 4)), synced(value, "long"))
        assertSame(value, synced(value, "long text"))
    }
}
