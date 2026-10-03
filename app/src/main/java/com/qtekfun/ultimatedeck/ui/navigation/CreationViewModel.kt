// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.board.BoardCreation
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.ui.boards.BoardSummary
import com.qtekfun.ultimatedeck.ui.boards.deckColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** What happened to a creation: the new board to open, or a message to show. */
sealed interface CreationEvent {
    data class BoardCreated(val board: BoardSummary) : CreationEvent

    data class Failed(val message: Int) : CreationEvent

    data object ColumnCreated : CreationEvent
}

/** Creates boards and columns; both need a connection (T15c). */
@HiltViewModel
class CreationViewModel @Inject constructor(private val creation: BoardCreation) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<CreationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CreationEvent> = mutableEvents.asSharedFlow()

    fun createBoard(title: String, color: String) {
        viewModelScope.launch {
            val result = creation.createBoard(title, color)
            mutableEvents.tryEmit(
                if (result is ApiResult.Success) {
                    CreationEvent.BoardCreated(
                        BoardSummary(
                            result.value.id,
                            result.value.title,
                            deckColor(result.value.color)
                        )
                    )
                } else {
                    result.failure()
                }
            )
        }
    }

    fun createColumn(boardId: Long, title: String) {
        viewModelScope.launch {
            val result = creation.createColumn(boardId, title)
            mutableEvents.tryEmit(
                if (result is ApiResult.Success) CreationEvent.ColumnCreated else result.failure()
            )
        }
    }

    private fun ApiResult<*>.failure() = CreationEvent.Failed(
        when (this) {
            is ApiResult.NetworkError -> R.string.create_needs_connection
            else -> R.string.create_failed
        }
    )
}
