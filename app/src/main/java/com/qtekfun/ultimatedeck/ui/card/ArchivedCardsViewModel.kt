// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.board.ArchivedCard
import com.qtekfun.ultimatedeck.data.board.ArchivedCardsRepository
import com.qtekfun.ultimatedeck.data.remote.ApiResult
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

/** Archived cards of one board, read from the server (T15b). */
@HiltViewModel
class ArchivedCardsViewModel @Inject constructor(private val repository: ArchivedCardsRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(ArchivedState())
    val state: StateFlow<ArchivedState> = mutableState.asStateFlow()
    private var boardId: Long? = null

    fun open(id: Long) {
        if (boardId == id) return
        boardId = id
        reload()
    }

    fun reload() {
        val id = boardId ?: return
        mutableState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = repository.load(id)
            mutableState.value = if (result is ApiResult.Success) {
                ArchivedState(loading = false, cards = result.value)
            } else {
                ArchivedState(loading = false, problem = result.toProblem())
            }
        }
    }

    fun restore(card: ArchivedCard) {
        viewModelScope.launch {
            val result = repository.unarchive(card)
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
