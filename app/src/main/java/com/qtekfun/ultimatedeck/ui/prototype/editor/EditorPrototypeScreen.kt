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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.editor.MarkdownEdits

private enum class EditorOption { LIVE_MARKDOWN, RICH_TEXT }

/**
 * Editor evaluation (T03): the same sample description in the live markdown editor (A) and in
 * compose-rich-editor (B), with the stored markdown shown to check nothing else changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorPrototypeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val resources = LocalResources.current
    val original = remember(resources) {
        resources.openRawResource(R.raw.sample_description).use { it.readBytes().decodeToString() }
    }
    var option by rememberSaveable { mutableStateOf(EditorOption.LIVE_MARKDOWN) }
    var showMarkdown by rememberSaveable { mutableStateOf(false) }
    var liveValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(original))
    }
    val richState = rememberRichTextState()
    LaunchedEffect(richState) { richState.setMarkdown(original) }

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
            OptionSelector(option) { option = it }
            val actions = when (option) {
                EditorOption.LIVE_MARKDOWN -> liveEditorActions(liveValue) { liveValue = it }
                EditorOption.RICH_TEXT -> richEditorActions(richState)
            }
            EditorToolbar(actions)
            when (option) {
                EditorOption.LIVE_MARKDOWN -> LiveMarkdownEditor(
                    value = liveValue,
                    onValueChange = { liveValue = it },
                    modifier = Modifier.fillMaxWidth()
                )

                EditorOption.RICH_TEXT -> RichTextEditor(
                    state = richState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val stored = when (option) {
                EditorOption.LIVE_MARKDOWN -> liveValue.text
                EditorOption.RICH_TEXT -> richMarkdown(richState)
            }
            StoredMarkdown(stored, original, showMarkdown) { showMarkdown = !showMarkdown }
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

/** compose-rich-editor has no checklists, so that action is unsupported. */
private fun richEditorActions(state: RichTextState) = EditorActions(
    bold = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
    italic = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
    bulletList = { state.toggleUnorderedList() },
    taskList = null
)

@Composable
private fun OptionSelector(selected: EditorOption, onSelect: (EditorOption) -> Unit) {
    val options = listOf(
        EditorOption.LIVE_MARKDOWN to R.string.editor_option_live,
        EditorOption.RICH_TEXT to R.string.editor_option_rich
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(stringResource(label)) }
        }
    }
}

/** Reads the annotated string first so the stored markdown recomposes on every edit. */
private fun richMarkdown(state: RichTextState): String {
    state.annotatedString
    return state.toMarkdown()
}

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
