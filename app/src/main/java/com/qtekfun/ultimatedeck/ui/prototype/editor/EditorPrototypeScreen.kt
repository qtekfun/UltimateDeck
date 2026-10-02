// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.editor.MarkdownEdits

/**
 * Editor prototype (T03): a sample description in the live markdown editor, with the stored
 * markdown shown to check that nothing but the edited text changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPrototypeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val resources = LocalResources.current
    val original = remember(resources) {
        resources.openRawResource(R.raw.sample_description).use { it.readBytes().decodeToString() }
    }
    var showMarkdown by rememberSaveable { mutableStateOf(false) }
    var liveValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { EditorTopBar(onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorToolbar(liveEditorActions(liveValue) { liveValue = it })
            LiveMarkdownEditor(
                value = liveValue,
                onValueChange = { liveValue = it },
                modifier = Modifier.fillMaxWidth()
            )
            StoredMarkdown(liveValue.text, original, showMarkdown) { showMarkdown = !showMarkdown }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.editor_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.editor_back))
            }
        }
    )
}

private fun liveEditorActions(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) =
    EditorActions(
        bold = { onChange(value.edit(MarkdownEdits::toggleBold)) },
        italic = { onChange(value.edit(MarkdownEdits::toggleItalic)) },
        bulletList = { onChange(value.edit(MarkdownEdits::toggleBulletList)) },
        taskList = { onChange(value.edit(MarkdownEdits::toggleTaskList)) }
    )

@Composable
private fun StoredMarkdown(
    stored: String,
    original: String,
    visible: Boolean,
    onToggle: () -> Unit
) {
    val identical = stored == original
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                if (identical) R.string.editor_identical else R.string.editor_changed
            ),
            style = MaterialTheme.typography.labelLarge,
            color = with(MaterialTheme.colorScheme) { if (identical) primary else error }
        )
        TextButton(onClick = onToggle) {
            Text(
                stringResource(
                    if (visible) R.string.editor_hide_markdown else R.string.editor_show_markdown
                )
            )
        }
        if (visible) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stored,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
