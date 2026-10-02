// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.editor.MarkdownEdits
import com.qtekfun.ultimatedeck.domain.editor.MarkdownListEdits

/**
 * Card description editor (T03, T14): the Markdown shown rendered, Jira style. Tapping it (or
 * the edit button) switches to the block editor; the stored Markdown can be checked at the foot.
 */
@Composable
fun CardEditor(
    original: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cardActions: @Composable RowScope.() -> Unit = {},
    onSourceChange: (String) -> Unit = {},
    header: @Composable () -> Unit = {}
) {
    var source by rememberSaveable { mutableStateOf(original) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var showMarkdown by rememberSaveable { mutableStateOf(false) }
    var activeText by remember { mutableStateOf<TextCommandTarget?>(null) }
    val tableTemplate = stringResource(R.string.editor_table_template)
    BackHandler { if (editing) editing = false else onBack() }
    LaunchedEffect(source) { onSourceChange(source) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            EditorTopBar(editing, onBack, { editing = !editing }, cardActions)
        },
        // Saved Markdown at the foot of the screen, out of the way of the text.
        bottomBar = {
            StoredMarkdown(source, original, showMarkdown) { showMarkdown = !showMarkdown }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header()
            if (editing) {
                EditorToolbar(
                    editorActions(activeText) {
                        source = withTable(source, tableTemplate)
                    }
                )
            }
            if (!editing && source.isBlank()) {
                TextButton(onClick = {
                    editing = true
                }) { Text(stringResource(R.string.editor_empty)) }
            }
            DescriptionFrame(editing) {
                MarkdownBlocks(
                    source = source,
                    editable = editing,
                    callbacks = MarkdownBlocksCallbacks(
                        onSourceChange = { source = it },
                        onActiveText = { activeText = it },
                        onTap = { editing = true }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Framed like the title field, so it is clear where the description starts and ends. */
@Composable
private fun DescriptionFrame(editing: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.editor_description_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .border(
                    width = if (editing) 2.dp else 1.dp,
                    color = if (editing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(12.dp)
        ) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    editing: Boolean,
    onBack: () -> Unit,
    onToggleEditing: () -> Unit,
    cardActions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        // The card title is right below, in its own field.
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.editor_back))
            }
        },
        actions = {
            IconButton(onClick = onToggleEditing) {
                if (editing) {
                    Icon(Icons.Filled.Check, stringResource(R.string.editor_done))
                } else {
                    Icon(Icons.Filled.Edit, stringResource(R.string.editor_edit))
                }
            }
            cardActions()
        }
    )
}

private fun editorActions(target: TextCommandTarget?, addTable: () -> Unit) = EditorActions(
    bold = { target?.apply(MarkdownEdits::toggleBold) },
    italic = { target?.apply(MarkdownEdits::toggleItalic) },
    heading = { target?.apply(MarkdownEdits::toggleHeading) },
    bulletList = { target?.apply(MarkdownListEdits::toggleBulletList) },
    taskList = { target?.apply(MarkdownListEdits::toggleTaskList) },
    addTable = addTable
)

/** Appends [table] as a new block, separated from the previous one by a blank line. */
private fun withTable(source: String, table: String): String {
    val separator = when {
        source.isEmpty() || source.endsWith("\n\n") -> ""
        source.endsWith("\n") -> "\n"
        else -> "\n\n"
    }
    return source + separator + table
}

@Composable
private fun StoredMarkdown(
    stored: String,
    original: String,
    visible: Boolean,
    onToggle: () -> Unit
) {
    val identical = stored == original
    Column(Modifier.navigationBarsPadding()) {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (identical) R.string.editor_identical else R.string.editor_changed
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onToggle) { Text(stringResource(R.string.editor_show_markdown)) }
        }
    }
    if (visible) {
        AlertDialog(
            onDismissRequest = onToggle,
            confirmButton = {
                TextButton(onClick = onToggle) {
                    Text(stringResource(R.string.editor_hide_markdown))
                }
            },
            text = {
                Text(
                    text = stored,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        )
    }
}
