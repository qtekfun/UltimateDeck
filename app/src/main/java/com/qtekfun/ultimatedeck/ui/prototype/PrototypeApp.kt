// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.ui.login.LoginScreen
import com.qtekfun.ultimatedeck.ui.prototype.board.BoardPrototypeScreen
import com.qtekfun.ultimatedeck.ui.prototype.editor.EditorPrototypeScreen
import com.qtekfun.ultimatedeck.ui.session.LogoutAction
import com.qtekfun.ultimatedeck.ui.session.SessionViewModel

/**
 * Minimal navigation until real navigation arrives with T11: login when nobody is signed in,
 * otherwise the board prototype and the editor.
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
    var openCard by rememberSaveable { mutableStateOf<Long?>(null) }
    if (openCard == null) {
        BoardPrototypeScreen(
            onOpenCard = { openCard = it },
            topBarActions = { LogoutAction(accountName, onLogOut) }
        )
    } else {
        EditorPrototypeScreen(onBack = { openCard = null })
    }
}
