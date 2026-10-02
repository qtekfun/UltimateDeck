// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.board.BoardContentRepository
import com.qtekfun.ultimatedeck.domain.board.BoardColumn
import com.qtekfun.ultimatedeck.domain.board.CardItem
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.domain.card.CardActions
import com.qtekfun.ultimatedeck.sync.engine.SyncEngine
import com.qtekfun.ultimatedeck.sync.engine.SyncProblem
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.toProblem
import com.qtekfun.ultimatedeck.ui.boards.deckColor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the board shows. [loading] is true until Room first answers. */
data class BoardState(
    val loading: Boolean = true,
    val columns: List<ColumnUi> = emptyList(),
    val syncing: Boolean = false,
    val problem: SyncProblem? = null
)

/**
 * A board read from Room (T12): it shows offline and updates after each sync. Moving cards only
 * changes the copy on screen until T13 saves the moves; the next update from Room resets them.
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val repository: BoardContentRepository,
    private val cardActions: CardActions,
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
                BoardState(false, columns.map { it.toUi() }, syncing, outcome.toProblem())
            }.collect { mutableState.value = it }
        }
    }

    private val mutableArchived = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** Cards just archived, each shown once in the "Undo" snackbar. */
    val archived: SharedFlow<Long> = mutableArchived.asSharedFlow()

    fun createCard(columnId: Long, title: String) {
        val board = boardId ?: return
        viewModelScope.launch { cardActions.create(board, columnId, title) }
    }

    fun archive(cardId: Long) {
        mutableArchived.tryEmit(cardId)
        viewModelScope.launch { cardActions.setArchived(cardId, true) }
    }

    /** Restores the card archived last, from the snackbar. */
    fun undoArchive(cardId: Long) {
        viewModelScope.launch { cardActions.setArchived(cardId, false) }
    }

    fun delete(cardId: Long) {
        viewModelScope.launch { cardActions.delete(cardId) }
    }

    /** Pull-to-refresh: syncs with the server; the board updates when Room changes. */
    fun refresh() = scheduler.requestSync()

    fun card(cardId: Long): CardUi? =
        state.value.columns.flatMap { it.cards }.firstOrNull { it.id == cardId }

    /** A drop on the board: shown at once, then saved and queued for the server. */
    fun moveCard(from: CardPosition, to: CardPosition) {
        val cardId = state.value.columns.getOrNull(from.column)?.cards?.getOrNull(from.index)?.id
        if (from == to || cardId == null) return
        mutableState.update { it.copy(columns = it.columns.withCardMoved(from, to)) }
        state.value.columns.getOrNull(to.column)?.let { column ->
            viewModelScope.launch {
                cardActions.move(cardId, column.id, column.cards.map { it.id })
            }
        }
    }

    /** Moves a card to the end of another column; used by accessibility actions. */
    fun moveCardToColumn(cardId: Long, column: Int) {
        val from = state.value.columns.positionOf(cardId) ?: return
        moveCard(from, CardPosition(column, Int.MAX_VALUE))
    }
}

private fun BoardColumn.toUi() = ColumnUi(id, title, cards.map { it.toUi() })

private fun CardItem.toUi(zone: ZoneId = ZoneId.systemDefault()) = CardUi(
    id = id,
    title = title,
    description = description,
    labels = labels.map { LabelUi(it.title, deckColor(it.color)) },
    assignees = assignees,
    dueDate = dueDate?.atZone(zone)?.toLocalDate(),
    attachments = attachments,
    checklistDone = checklistDone,
    checklistTotal = checklistTotal,
    pendingSync = pendingSync
)
