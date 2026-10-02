// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.board.BoardContentRepository
import com.qtekfun.ultimatedeck.domain.board.BoardColumn
import com.qtekfun.ultimatedeck.domain.board.CardItem
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.sync.engine.SyncEngine
import com.qtekfun.ultimatedeck.sync.engine.SyncProblem
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.toProblem
import com.qtekfun.ultimatedeck.ui.prototype.remote.deckColor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the board shows. [loading] is true until Room first answers. */
data class BoardState(
    val loading: Boolean = true,
    val columns: List<PrototypeColumn> = emptyList(),
    val syncing: Boolean = false,
    val problem: SyncProblem? = null
)

/**
 * A board read from Room (T12): it shows offline and updates after each sync. Moving cards only
 * changes the copy on screen until T13 saves the moves; the next update from Room resets them.
 */
@HiltViewModel
class BoardPrototypeViewModel @Inject constructor(
    private val repository: BoardContentRepository,
    private val engine: SyncEngine,
    private val scheduler: SyncScheduler
) : ViewModel() {
    private val mutableState = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = mutableState.asStateFlow()
    private var boardId: Long? = null
    private var observing: Job? = null

    /** Shows [id]; opening the same board again (e.g. after rotation) keeps the state. */
    fun open(id: Long) {
        if (boardId == id) return
        boardId = id
        observing?.cancel()
        observing = viewModelScope.launch {
            combine(
                repository.observeBoard(id),
                scheduler.syncing(),
                engine.lastOutcome
            ) { columns, syncing, outcome ->
                BoardState(false, columns.map { it.toPrototype() }, syncing, outcome.toProblem())
            }.collect { mutableState.value = it }
        }
    }

    /** Pull-to-refresh: syncs with the server; the board updates when Room changes. */
    fun refresh() = scheduler.requestSync()

    fun card(cardId: Long): PrototypeCard? =
        state.value.columns.flatMap { it.cards }.firstOrNull { it.id == cardId }

    fun moveCard(from: CardPosition, to: CardPosition) = updateColumns {
        it.withCardMoved(from, to)
    }

    /** Moves a card to the end of another column; used by accessibility actions. */
    fun moveCardToColumn(cardId: Long, column: Int) = updateColumns { columns ->
        columns.positionOf(cardId)?.let {
            columns.withCardMoved(it, CardPosition(column, Int.MAX_VALUE))
        } ?: columns
    }

    private fun updateColumns(change: (List<PrototypeColumn>) -> List<PrototypeColumn>) {
        mutableState.update { it.copy(columns = change(it.columns)) }
    }
}

private fun BoardColumn.toPrototype() = PrototypeColumn(id, title, cards.map { it.toPrototype() })

private fun CardItem.toPrototype(zone: ZoneId = ZoneId.systemDefault()) = PrototypeCard(
    id = id,
    title = title,
    description = description,
    labels = labels.map { PrototypeLabel(it.title, deckColor(it.color)) },
    assignees = assignees,
    dueDate = dueDate?.atZone(zone)?.toLocalDate(),
    attachments = attachments,
    checklistDone = checklistDone,
    checklistTotal = checklistTotal,
    pendingSync = pendingSync
)
