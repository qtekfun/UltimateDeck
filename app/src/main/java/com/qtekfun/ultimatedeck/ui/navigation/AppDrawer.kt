// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.boards.BoardSummary

/** Side menu (T18b): the boards, each can be the one that opens at start, and the settings. */
@Composable
fun AppDrawer(
    accountName: String,
    boards: List<BoardSummary>,
    currentBoardId: Long?,
    favoriteBoardId: Long?,
    callbacks: DrawerCallbacks
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Text(
                accountName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                stringResource(R.string.drawer_boards),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() }
            )
            boards.forEach { board ->
                BoardItem(
                    board = board,
                    selected = board.id == currentBoardId,
                    favorite = board.id == favoriteBoardId,
                    callbacks = callbacks
                )
            }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.board_new_title)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                selected = false,
                onClick = callbacks.onNewBoard
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings_title)) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = false,
                onClick = callbacks.onSettings
            )
        }
    }
}

@Composable
private fun BoardItem(
    board: BoardSummary,
    selected: Boolean,
    favorite: Boolean,
    callbacks: DrawerCallbacks
) {
    val action = if (favorite) {
        stringResource(R.string.drawer_unfavorite, board.title)
    } else {
        stringResource(R.string.drawer_favorite, board.title)
    }
    NavigationDrawerItem(
        label = { Text(board.title) },
        icon = { Box(Modifier.size(12.dp).background(board.color, CircleShape)) },
        badge = {
            IconButton(onClick = { callbacks.onFavorite(if (favorite) null else board.id) }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = action,
                    tint = if (favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            }
        },
        selected = selected,
        onClick = { callbacks.onOpenBoard(board) }
    )
}
