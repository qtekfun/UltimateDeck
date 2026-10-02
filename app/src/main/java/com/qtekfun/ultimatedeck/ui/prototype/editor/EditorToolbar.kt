// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R

@Composable
fun EditorToolbar(actions: EditorActions, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolbarButton(
            R.string.editor_bold_short,
            R.string.editor_bold,
            actions.bold,
            FontWeight.Bold
        )
        ToolbarButton(
            R.string.editor_italic_short,
            R.string.editor_italic,
            actions.italic,
            fontStyle = FontStyle.Italic
        )
        ToolbarButton(
            R.string.editor_bullet_list_short,
            R.string.editor_bullet_list,
            actions.bulletList
        )
        ToolbarButton(R.string.editor_task_list_short, R.string.editor_task_list, actions.taskList)
    }
}

@Composable
private fun ToolbarButton(
    label: Int,
    description: Int,
    onClick: (() -> Unit)?,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null
) {
    val text = stringResource(description)
    val unsupported = stringResource(R.string.editor_not_supported)
    FilledTonalButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = if (onClick != null) text else "$text. $unsupported" }
    ) {
        Text(stringResource(label), fontWeight = fontWeight, fontStyle = fontStyle)
    }
}
