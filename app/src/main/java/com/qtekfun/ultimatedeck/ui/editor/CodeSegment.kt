// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.editor.CodeBlock

private val BlockShape = RoundedCornerShape(8.dp)

/** A code block showing only its content; the fences stay hidden and are kept on save. */
@Composable
fun CodeSegment(
    text: String,
    editable: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val block = remember(text) { CodeBlock.parse(text) }
    val label = block.language.ifEmpty { stringResource(R.string.editor_code) }
    MonospaceBlock(
        text = block.content,
        label = label,
        editable = editable,
        onChange = { onChange(block.withContent(it).serialize()) },
        modifier = modifier
    )
}

/** HTML and other blocks the editor does not understand: shown and edited verbatim. */
@Composable
fun RawSegment(
    text: String,
    editable: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MonospaceBlock(
        text = text.trimEnd('\n', '\r'),
        label = stringResource(R.string.editor_raw),
        editable = editable,
        onChange = { onChange(it + text.substring(text.trimEnd('\n', '\r').length)) },
        modifier = modifier
    )
}

@Composable
private fun MonospaceBlock(
    text: String,
    label: String,
    editable: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BlockShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (editable) MonospaceField(text, style, onChange) else Text(text, style = style)
        }
    }
}

@Composable
private fun MonospaceField(text: String, style: TextStyle, onChange: (String) -> Unit) {
    var local by remember { mutableStateOf(TextFieldValue(text)) }
    BasicTextField(
        value = synced(local, text),
        onValueChange = {
            local = it
            if (it.text != text) onChange(it.text)
        },
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    )
}
