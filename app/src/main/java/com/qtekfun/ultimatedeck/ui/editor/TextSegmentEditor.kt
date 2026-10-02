// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Editor of a text segment: rendered markdown without symbols. It keeps its own cursor and
 * reports itself as the toolbar's target when focused.
 */
@Composable
fun TextSegmentEditor(
    text: String,
    onTextChange: (String) -> Unit,
    onActive: (TextCommandTarget) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    var local by remember { mutableStateOf(TextFieldValue(text)) }
    val value = synced(local, text)
    val currentValue by rememberUpdatedState(value)
    val currentTextChange by rememberUpdatedState(onTextChange)
    val target = remember {
        TextCommandTarget { command ->
            val edited = currentValue.edit(command)
            local = edited
            currentTextChange(edited.text)
        }
    }
    LiveMarkdownEditor(
        value = value,
        onValueChange = {
            local = it
            if (it.text != text) onTextChange(it.text)
        },
        singleLine = singleLine,
        modifier = modifier.onFocusChanged { if (it.isFocused) onActive(target) }
    )
}
