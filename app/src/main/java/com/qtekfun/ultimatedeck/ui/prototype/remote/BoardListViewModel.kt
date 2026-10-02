// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** The account's boards, fetched online (T06 preview; T11 reads them from Room). */
@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val apis: AccountApiProvider,
    private val scheduler: SyncScheduler
) : ViewModel() {
    private val mutableState = MutableStateFlow<RemoteLoad<List<BoardSummary>>>(RemoteLoad.Loading)
    val state: StateFlow<RemoteLoad<List<BoardSummary>>> = mutableState.asStateFlow()

    /** A sync started by pull-to-refresh is running. */
    val syncing: StateFlow<Boolean> = scheduler.syncing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        reload()
    }

    /** Pull-to-refresh: syncs with the server and reloads the list. */
    fun refresh() {
        scheduler.requestSync()
        reload()
    }

    fun reload() {
        mutableState.value = RemoteLoad.Loading
        viewModelScope.launch {
            val api = apis.api()
            val result = if (api ==
                null
            ) {
                ApiResult.Unauthorized
            } else {
                apiCall { api.boards.getBoards() }
            }
            mutableState.value = when (result) {
                is ApiResult.Success -> RemoteLoad.Loaded(result.value.toSummaries())
                else -> RemoteLoad.Failed(result.toRemoteError())
            }
        }
    }
}
