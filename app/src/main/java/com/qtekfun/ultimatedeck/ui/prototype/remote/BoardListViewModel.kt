// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The account's boards, fetched online (T06 preview; T11 reads them from Room). */
@HiltViewModel
class BoardListViewModel @Inject constructor(private val apis: AccountApiProvider) : ViewModel() {
    private val mutableState = MutableStateFlow<RemoteLoad<List<BoardSummary>>>(RemoteLoad.Loading)
    val state: StateFlow<RemoteLoad<List<BoardSummary>>> = mutableState.asStateFlow()

    init {
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
