// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.board.ArchivedCard

/** The archived cards of a board, each with a button to restore it (T15b). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedCardsScreen(
    boardId: Long,
    onBack: () -> Unit,
    viewModel: ArchivedCardsViewModel = viewModel()
) {
    LaunchedEffect(boardId) { viewModel.open(boardId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.board_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::reload,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.failed) item { FailedText() }
                if (!state.loading && !state.failed && state.cards.isEmpty()) {
                    item { Text(stringResource(R.string.archived_empty), Modifier.padding(24.dp)) }
                }
                items(state.cards, key = { it.id }) { card ->
                    ArchivedRow(card, onRestore = { viewModel.restore(card) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FailedText() {
    Text(
        text = stringResource(R.string.archived_failed),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun ArchivedRow(card: ArchivedCard, onRestore: () -> Unit) {
    val restore = stringResource(R.string.archived_restore_card, card.title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(
                card.column,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onRestore,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = restore }
        ) { Text(stringResource(R.string.archived_restore)) }
    }
}
