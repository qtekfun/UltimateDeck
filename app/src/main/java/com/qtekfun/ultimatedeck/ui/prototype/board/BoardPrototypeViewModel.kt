// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.ui.prototype.remote.RemoteLoad
import com.qtekfun.ultimatedeck.ui.prototype.remote.toColumns
import com.qtekfun.ultimatedeck.ui.prototype.remote.toRemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A real board loaded from the server, read-only (T06 preview): moves only change this copy in
 * memory and are lost when leaving the board. Saving and syncing come with T09 and T13.
 */
@HiltViewModel
class BoardPrototypeViewModel @Inject constructor(private val apis: AccountApiProvider) :
    ViewModel() {
    private val mutableState =
        MutableStateFlow<RemoteLoad<List<PrototypeColumn>>>(RemoteLoad.Loading)
    val state: StateFlow<RemoteLoad<List<PrototypeColumn>>> = mutableState.asStateFlow()
    private var boardId: Long? = null

    /** Loads [id] once; the loaded copy survives rotation. */
    fun open(id: Long) {
        if (boardId == id) return
        boardId = id
        reload()
    }

    fun reload() {
        val id = boardId ?: return
        mutableState.value = RemoteLoad.Loading
        viewModelScope.launch {
            val api = apis.api()
            val result = if (api ==
                null
            ) {
                ApiResult.Unauthorized
            } else {
                apiCall { api.boards.getStacks(id) }
            }
            mutableState.value = when (result) {
                is ApiResult.Success -> RemoteLoad.Loaded(result.value.toColumns())
                else -> RemoteLoad.Failed(result.toRemoteError())
            }
        }
    }

    fun card(cardId: Long): PrototypeCard? =
        (state.value as? RemoteLoad.Loaded)?.value?.flatMap { it.cards }?.firstOrNull {
            it.id ==
                cardId
        }

    fun moveCard(from: CardPosition, to: CardPosition) = updateColumns {
        it.withCardMoved(from, to)
    }

    /** Moves a card to the end of another column; used by accessibility actions. */
    fun moveCardToColumn(cardId: Long, column: Int) = updateColumns { columns ->
        columns.positionOf(cardId)?.let {
            columns.withCardMoved(it, CardPosition(column, Int.MAX_VALUE))
        }
            ?: columns
    }

    private fun updateColumns(change: (List<PrototypeColumn>) -> List<PrototypeColumn>) {
        mutableState.update { current ->
            if (current is RemoteLoad.Loaded) RemoteLoad.Loaded(change(current.value)) else current
        }
    }
}
