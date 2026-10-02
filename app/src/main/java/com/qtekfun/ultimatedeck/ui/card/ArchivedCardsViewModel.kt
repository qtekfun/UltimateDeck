// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.board.ArchivedCard
import com.qtekfun.ultimatedeck.data.board.ArchivedCardsRepository
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.domain.card.CardActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the archived cards could not be loaded or restored. */
enum class ArchivedProblem { OFFLINE, FAILED }

data class ArchivedState(
    val loading: Boolean = true,
    val cards: List<ArchivedCard> = emptyList(),
    val problem: ArchivedProblem? = null
)

/** Archived cards of one board: from the server, plus those archived here (T15b). */
@HiltViewModel
class ArchivedCardsViewModel @Inject constructor(
    private val repository: ArchivedCardsRepository,
    private val cardActions: CardActions
) : ViewModel() {
    private val mutableState = MutableStateFlow(ArchivedState())
    val state: StateFlow<ArchivedState> = mutableState.asStateFlow()
    private var boardId: Long? = null

    /** Loads again on every visit: cards may have been archived since the last one. */
    fun open(id: Long) {
        if (boardId != id) mutableState.value = ArchivedState()
        boardId = id
        reload()
    }

    fun reload() {
        val id = boardId ?: return
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = repository.load(id)
            mutableState.value = ArchivedState(
                loading = false,
                cards = result.cards,
                problem = result.failure?.toProblem()
            )
        }
    }

    fun restore(card: ArchivedCard) {
        viewModelScope.launch {
            // Archived here: restored here too, offline if needed, like archiving.
            val result = if (card.local) {
                cardActions.setArchived(card.id, archived = false)
                ApiResult.Success(Unit)
            } else {
                repository.unarchive(card)
            }
            mutableState.update { state ->
                if (result is ApiResult.Success) {
                    state.copy(cards = state.cards - card, problem = null)
                } else {
                    state.copy(problem = result.toProblem())
                }
            }
        }
    }

    private fun ApiResult<*>.toProblem() =
        if (this is ApiResult.NetworkError) ArchivedProblem.OFFLINE else ArchivedProblem.FAILED
}
