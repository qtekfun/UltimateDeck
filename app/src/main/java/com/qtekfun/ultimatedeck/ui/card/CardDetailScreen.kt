// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.board.DeleteCardDialog
import com.qtekfun.ultimatedeck.ui.editor.CardEditor

/** Card detail (T14): title and description edited in place and saved automatically. */
@Composable
fun CardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    viewModel: CardDetailViewModel = viewModel()
) {
    LaunchedEffect(cardId) { viewModel.open(cardId) }
    val card by viewModel.card.collectAsStateWithLifecycle()
    val revision by viewModel.revision.collectAsStateWithLifecycle()
    val current = card?.takeIf { it.id == cardId } ?: return
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        DeleteCardDialog(onDelete = onDelete, onDismiss = { confirmDelete = false })
    }
    current.conflicts.firstOrNull()?.takeIf { reviewing }?.let { conflict ->
        ConflictDialog(
            conflict = conflict,
            onResolve = { keepMine -> viewModel.resolve(conflict.field, keepMine) },
            onDismiss = { reviewing = false }
        )
    }
    key(cardId, revision) {
        var title by rememberSaveable { mutableStateOf(current.title) }
        var description by rememberSaveable { mutableStateOf(current.description) }
        CardEditor(
            original = current.description,
            onBack = {
                viewModel.saveNow(title, description)
                onBack()
            },
            cardActions = {
                TextButton(onClick = onArchive) { Text(stringResource(R.string.card_archive)) }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.card_delete))
                }
            },
            onSourceChange = {
                description = it
                viewModel.onDescriptionChange(it)
            },
            header = {
                CardHeader(
                    title = title,
                    onTitleChange = {
                        title = it
                        viewModel.onTitleChange(it)
                    },
                    card = current,
                    onReview = { reviewing = true }
                )
                CardMetadata(
                    card = current,
                    onDueDate = viewModel::setDueDate,
                    onLabels = viewModel::setLabels,
                    onAssignees = viewModel::setAssignees
                )
                AttachmentsSection(cardId = current.id)
            }
        )
    }
}

@Composable
private fun CardHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    card: CardDetail,
    onReview: () -> Unit
) {
    Column {
        if (card.conflicts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.card_conflict_banner),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReview) {
                    Text(stringResource(R.string.card_conflict_review))
                }
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.card_title_label)) },
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        if (card.pendingSync) {
            Text(
                text = stringResource(R.string.card_pending_sync),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
