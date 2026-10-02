// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.notify.CardLink
import com.qtekfun.ultimatedeck.ui.board.BoardScreen
import com.qtekfun.ultimatedeck.ui.board.BoardViewModel
import com.qtekfun.ultimatedeck.ui.boards.BoardListScreen
import com.qtekfun.ultimatedeck.ui.boards.BoardListViewModel
import com.qtekfun.ultimatedeck.ui.boards.BoardSummary
import com.qtekfun.ultimatedeck.ui.card.ArchivedCardsScreen
import com.qtekfun.ultimatedeck.ui.card.CardDetailScreen
import com.qtekfun.ultimatedeck.ui.login.LoginScreen
import com.qtekfun.ultimatedeck.ui.navigation.AppDrawer
import com.qtekfun.ultimatedeck.ui.navigation.DrawerCallbacks
import com.qtekfun.ultimatedeck.ui.navigation.startBoard
import com.qtekfun.ultimatedeck.ui.session.SessionViewModel
import com.qtekfun.ultimatedeck.ui.settings.SettingsScreen
import com.qtekfun.ultimatedeck.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Minimal navigation until real navigation arrives with T11: login when nobody is signed in,
 * otherwise the account's boards, a board and a card description.
 */
@Composable
fun AppNavigation(
    link: CardLink? = null,
    onLinkOpened: () -> Unit = {},
    sessionViewModel: SessionViewModel = viewModel()
) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    val account = session.account
    when {
        !session.ready -> Unit
        account == null -> LoginScreen()
        else -> SignedIn(account, link, onLinkOpened, onLogOut = sessionViewModel::logOut)
    }
}

/** Where the signed-in user is: a board, its archived cards, a card, or the settings. */
private class Place {
    var boardId by mutableStateOf<Long?>(null)
    var boardTitle by mutableStateOf("")
    var cardId by mutableStateOf<Long?>(null)
    var archived by mutableStateOf(false)
    var settings by mutableStateOf(false)

    fun open(board: BoardSummary) {
        boardId = board.id
        boardTitle = board.title
        cardId = null
        archived = false
        settings = false
    }

    companion object {
        /** Keeps the place across rotation and process death, like the rest of the UI state. */
        val Saver = listSaver<Place, Any?>(
            save = { listOf(it.boardId, it.boardTitle, it.cardId, it.archived, it.settings) },
            restore = { saved ->
                Place().apply {
                    boardId = saved[0] as Long?
                    boardTitle = saved[1] as String
                    cardId = saved[2] as Long?
                    archived = saved[3] as Boolean
                    settings = saved[4] as Boolean
                }
            }
        )
    }
}

@Composable
private fun SignedIn(
    account: AccountEntity,
    link: CardLink?,
    onLinkOpened: () -> Unit,
    onLogOut: () -> Unit
) {
    val place = rememberSaveable(saver = Place.Saver) { Place() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val boardsViewModel: BoardListViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val boards by boardsViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        val favorite = settingsViewModel.storedFavorite()
        val active = boardsViewModel.state.first { !it.loading }.boards
        // A card opened from a notification wins over the favorite board.
        if (place.boardId == null) startBoard(favorite, active)?.let(place::open)
        started = true
    }
    LaunchedEffect(link) {
        link?.let {
            place.open(BoardSummary(it.boardId, it.boardTitle, Color.Unspecified))
            place.cardId = it.cardId
            onLinkOpened()
        }
    }
    val closeThen: (() -> Unit) -> Unit = { action ->
        action()
        scope.launch { drawer.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawer,
        // Opened from the menu button only: a swipe would fight the board's columns.
        gesturesEnabled = drawer.isOpen,
        drawerContent = {
            AppDrawer(
                accountName = account.displayName,
                boards = boards.boards,
                currentBoardId = place.boardId,
                favoriteBoardId = settings.favoriteBoardId,
                callbacks = DrawerCallbacks(
                    onOpenBoard = { board -> closeThen { place.open(board) } },
                    onFavorite = settingsViewModel::setFavoriteBoard,
                    onSettings = { closeThen { place.settings = true } }
                )
            )
        }
    ) {
        SignedInContent(account, place, onLogOut) { scope.launch { drawer.open() } }
    }
}

@Composable
private fun SignedInContent(
    account: AccountEntity,
    place: Place,
    onLogOut: () -> Unit,
    onMenu: () -> Unit
) {
    val boardViewModel: BoardViewModel = viewModel()
    val board = place.boardId
    val card = place.cardId
    when {
        place.settings -> {
            BackHandler { place.settings = false }
            SettingsScreen(
                accountName = account.displayName,
                server = account.serverUrl,
                onLogOut = onLogOut,
                onBack = { place.settings = false }
            )
        }

        board == null -> BoardListScreen(onOpenBoard = place::open, onMenu = onMenu)

        place.archived -> {
            BackHandler { place.archived = false }
            ArchivedCardsScreen(boardId = board, onBack = { place.archived = false })
        }

        card == null -> {
            BackHandler { place.boardId = null }
            LaunchedEffect(board) { boardViewModel.open(board) }
            BoardScreen(
                title = place.boardTitle,
                viewModel = boardViewModel,
                onMenu = onMenu,
                onOpenCard = { place.cardId = it },
                onShowArchived = { place.archived = true }
            )
        }

        else -> CardDetailScreen(
            cardId = card,
            onBack = { place.cardId = null },
            onArchive = {
                boardViewModel.archive(card)
                place.cardId = null
            },
            onDelete = {
                boardViewModel.delete(card)
                place.cardId = null
            }
        )
    }
}
