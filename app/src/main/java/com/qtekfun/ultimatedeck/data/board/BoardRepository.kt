// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.domain.board.BoardItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Boards of the signed-in account, read from Room (RF-08): they work offline. */
class BoardRepository @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase
) {
    private val boards = database.boardDao()

    /** Active boards by title; empty without an account. Updates after every sync. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeBoards(): Flow<List<BoardItem>> = session.activeAccount.flatMapLatest { account ->
        if (account == null) {
            flowOf(emptyList())
        } else {
            boards.observeActive(account.id).map { list ->
                list.map { BoardItem(it.id, it.title, it.color) }
            }
        }
    }
}
