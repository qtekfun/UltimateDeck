// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.board.BoardRepository
import com.qtekfun.ultimatedeck.sync.engine.SyncEngine
import com.qtekfun.ultimatedeck.sync.engine.SyncProblem
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.toProblem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MS = 5_000L

/** What the board list shows. [loading] is true until Room first answers. */
data class BoardListState(
    val loading: Boolean = true,
    val boards: List<BoardSummary> = emptyList(),
    val syncing: Boolean = false,
    val problem: SyncProblem? = null
)

/** The account's boards from Room (T11): they show offline and update after each sync. */
@HiltViewModel
class BoardListViewModel @Inject constructor(
    repository: BoardRepository,
    engine: SyncEngine,
    private val scheduler: SyncScheduler
) : ViewModel() {
    val state: StateFlow<BoardListState> = combine(
        repository.observeBoards(),
        scheduler.syncing(),
        engine.lastOutcome
    ) { boards, syncing, outcome ->
        BoardListState(
            loading = false,
            boards = boards.map { BoardSummary(it.id, it.title, deckColor(it.color)) },
            syncing = syncing,
            problem = outcome.toProblem()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BoardListState())

    /** Pull-to-refresh: syncs with the server; the list updates when Room changes. */
    fun refresh() = scheduler.requestSync()
}
