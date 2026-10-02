// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import com.qtekfun.ultimatedeck.domain.editor.MarkdownEdits

/**
 * Read-only rendered markdown. Tapping a checkbox calls [onSourceChange] with it toggled;
 * tapping anywhere else calls [onTap].
 */
@Composable
fun MarkdownText(
    source: String,
    modifier: Modifier = Modifier,
    onSourceChange: ((String) -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    val styles = rememberLiveMarkdownStyles()
    val document = remember(source) { MarkdownDocument.parse(source) }
    val shown = remember(document, styles) {
        LiveMarkdownTransformation(document, TextRange.Zero, styles).filter(AnnotatedString(source))
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentSourceChange by rememberUpdatedState(onSourceChange)
    val currentTap by rememberUpdatedState(onTap)
    BasicText(
        text = shown.text,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(document) {
            detectTapGestures { position ->
                val offset = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                val original = shown.offsetMapping.transformedToOriginal(offset)
                val task = document.tasks.firstOrNull { original in it.range.start..it.range.end }
                val toggle = currentSourceChange
                if (task != null && toggle != null) {
                    toggle(MarkdownEdits.toggleTask(source, task))
                } else {
                    currentTap?.invoke()
                }
            }
        }
    )
}
