// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.sync.engine.SyncProblem

/** "Your boards": the account's boards from Room, so they also show offline (T11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    onOpenBoard: (BoardSummary) -> Unit,
    modifier: Modifier = Modifier,
    onMenu: () -> Unit = {},
    viewModel: BoardListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.boards_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Filled.Menu, stringResource(R.string.drawer_menu))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.boards_refresh))
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.loading -> Unit

                state.boards.isEmpty() -> EmptyBoards(state)

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    state.problem?.let { problem -> item { SyncProblemBanner(problem) } }
                    items(state.boards, key = { it.id }) { board ->
                        BoardRow(board, onClick = { onOpenBoard(board) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Scrollable so that the pull gesture also works without boards. */
@Composable
private fun EmptyBoards(state: BoardListState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.problem?.let { SyncProblemBanner(it) }
        val message = if (state.syncing) R.string.boards_syncing else R.string.boards_empty
        Text(stringResource(message), textAlign = TextAlign.Center)
    }
}

/** Why the last sync failed, above the content. */
@Composable
fun SyncProblemBanner(problem: SyncProblem) {
    val message = when (problem) {
        SyncProblem.OFFLINE -> R.string.sync_problem_offline
        SyncProblem.UNAUTHORIZED -> R.string.remote_error_unauthorized
        SyncProblem.SERVER -> R.string.sync_problem_server
    }
    Text(
        text = stringResource(message),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun BoardRow(board: BoardSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(board.color, CircleShape)
        )
        Text(board.title, style = MaterialTheme.typography.titleMedium)
    }
}
