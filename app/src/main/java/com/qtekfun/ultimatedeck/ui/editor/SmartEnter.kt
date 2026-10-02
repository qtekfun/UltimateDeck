// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.qtekfun.ultimatedeck.domain.editor.MarkdownListEdits

/**
 * If the change from [old] to [new] is a single line break typed at the cursor, applies
 * [MarkdownListEdits.smartEnter] to continue or end the list instead. Otherwise returns [new].
 */
internal fun applySmartEnter(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val cursor = old.selection.min
    val typedBreak = old.selection.collapsed &&
        new.text.length == old.text.length + 1 &&
        new.text.getOrNull(cursor) == '\n' &&
        new.text.removeRange(cursor, cursor + 1) == old.text
    val result = if (typedBreak) MarkdownListEdits.smartEnter(old.text, cursor) else null
    return result?.let { TextFieldValue(it.text, TextRange(it.selection.start, it.selection.end)) }
        ?: new
}

/** [value] with its text replaced by [text] when they differ, keeping the selection in range. */
internal fun synced(value: TextFieldValue, text: String): TextFieldValue = if (value.text == text) {
    value
} else {
    TextFieldValue(
        text,
        TextRange(
            value.selection.start.coerceAtMost(text.length),
            value.selection.end.coerceAtMost(text.length)
        )
    )
}
