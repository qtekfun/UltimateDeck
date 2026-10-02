// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.card.ArchivedCardsScreen
import com.qtekfun.ultimatedeck.ui.card.CardDetailScreen
import com.qtekfun.ultimatedeck.ui.login.LoginScreen
import com.qtekfun.ultimatedeck.ui.prototype.board.BoardPrototypeScreen
import com.qtekfun.ultimatedeck.ui.prototype.board.BoardPrototypeViewModel
import com.qtekfun.ultimatedeck.ui.prototype.editor.EditorPrototypeScreen
import com.qtekfun.ultimatedeck.ui.prototype.remote.BoardListScreen
import com.qtekfun.ultimatedeck.ui.session.LogoutAction
import com.qtekfun.ultimatedeck.ui.session.SessionViewModel

/**
 * Minimal navigation until real navigation arrives with T11: login when nobody is signed in,
 * otherwise the account's boards, a board and a card description.
 */
@Composable
fun PrototypeApp(sessionViewModel: SessionViewModel = viewModel()) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    val account = session.account
    when {
        !session.ready -> Unit
        account == null -> LoginScreen()
        else -> SignedIn(accountName = account.displayName, onLogOut = sessionViewModel::logOut)
    }
}

@Composable
private fun SignedIn(accountName: String, onLogOut: () -> Unit) {
    var boardId by rememberSaveable { mutableStateOf<Long?>(null) }
    var boardTitle by rememberSaveable { mutableStateOf("") }
    var cardId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val boardViewModel: BoardPrototypeViewModel = viewModel()
    val board = boardId
    val card = cardId
    when {
        board == null -> BoardListScreen(
            onOpenBoard = {
                boardId = it.id
                boardTitle = it.title
            },
            topBarActions = { LogoutAction(accountName, onLogOut) }
        )

        showArchived -> {
            BackHandler { showArchived = false }
            ArchivedCardsScreen(boardId = board, onBack = { showArchived = false })
        }

        card == null -> {
            BackHandler { boardId = null }
            LaunchedEffect(board) { boardViewModel.open(board) }
            BoardPrototypeScreen(
                title = boardTitle,
                viewModel = boardViewModel,
                onBack = { boardId = null },
                onOpenCard = { cardId = it },
                onShowArchived = { showArchived = true }
            )
        }

        else -> CardDetailScreen(
            cardId = card,
            onBack = { cardId = null },
            onArchive = {
                boardViewModel.archive(card)
                cardId = null
            },
            onDelete = {
                boardViewModel.delete(card)
                cardId = null
            }
        )
    }
}
