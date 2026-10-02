// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.qtekfun.ultimatedeck.domain.editor.EditResult
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import com.qtekfun.ultimatedeck.domain.editor.MarkdownEdits
import com.qtekfun.ultimatedeck.domain.editor.SourceRange
import com.qtekfun.ultimatedeck.domain.editor.TaskMarker

/**
 * A text field whose content is the markdown source, displayed rendered by
 * [LiveMarkdownTransformation]. Tapping a checkbox ticks it in the source, and Enter continues
 * or ends lists.
 */
@Composable
fun LiveMarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    val document = remember(value.text) { MarkdownDocument.parse(value.text) }
    val styles = rememberLiveMarkdownStyles()
    val transformation = remember(document, value.selection, styles) {
        LiveMarkdownTransformation(document, value.selection, styles)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentValue by rememberUpdatedState(value)
    val currentDocument by rememberUpdatedState(document)
    // The gesture detector below is installed once, so it must read the current transformation:
    // its offset mapping depends on the cursor position.
    val currentTransformation by rememberUpdatedState(transformation)

    fun taskAt(position: Offset): TaskMarker? {
        val textLayout = layout ?: return null
        val original = currentTransformation.mapping.transformedToOriginal(
            textLayout.getOffsetForPosition(position)
        )
        return currentDocument.tasks.firstOrNull { original in it.range.start..it.range.end }
    }

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(applySmartEnter(currentValue, it)) },
        visualTransformation = transformation,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                val task = taskAt(down.position) ?: return@awaitEachGesture
                down.consume()
                val up =
                    waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                up.consume()
                val text = MarkdownEdits.toggleTask(currentValue.text, task)
                onValueChange(currentValue.copy(text = text))
            }
        }
    )
}

/** Applies a formatting command of [MarkdownEdits] to the field's text and selection. */
internal fun TextFieldValue.edit(command: (String, SourceRange) -> EditResult): TextFieldValue {
    val result = command(text, SourceRange(selection.min, selection.max))
    return TextFieldValue(result.text, TextRange(result.selection.start, result.selection.end))
}
