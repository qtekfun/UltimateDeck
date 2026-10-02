// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
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

/** One toolbar button: the short glyph shown and the full name read by TalkBack. */
private data class ToolbarItem(
    @param:StringRes val label: Int,
    @param:StringRes val description: Int,
    val onClick: () -> Unit,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null
)

@Composable
fun EditorToolbar(actions: EditorActions, modifier: Modifier = Modifier) {
    val items = listOf(
        ToolbarItem(
            R.string.editor_bold_short,
            R.string.editor_bold,
            actions.bold,
            fontWeight = FontWeight.Bold
        ),
        ToolbarItem(
            R.string.editor_italic_short,
            R.string.editor_italic,
            actions.italic,
            fontStyle = FontStyle.Italic
        ),
        ToolbarItem(
            R.string.editor_heading_short,
            R.string.editor_heading,
            actions.heading,
            FontWeight.Bold
        ),
        ToolbarItem(
            R.string.editor_bullet_list_short,
            R.string.editor_bullet_list,
            actions.bulletList
        ),
        ToolbarItem(R.string.editor_task_list_short, R.string.editor_task_list, actions.taskList),
        ToolbarItem(R.string.editor_add_table_short, R.string.editor_add_table, actions.addTable)
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val description = stringResource(item.description)
            FilledTonalButton(
                onClick = item.onClick,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = description }
            ) {
                Text(
                    stringResource(item.label),
                    fontWeight = item.fontWeight,
                    fontStyle = item.fontStyle
                )
            }
        }
    }
}
