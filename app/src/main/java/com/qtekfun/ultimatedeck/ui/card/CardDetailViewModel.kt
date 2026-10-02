// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.domain.card.CardActions
import com.qtekfun.ultimatedeck.domain.card.textConflicts
import com.qtekfun.ultimatedeck.sync.conflict.TextConflict
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pause after the last keystroke before an edit is saved. */
internal const val SAVE_DELAY_MS = 800L

/** The card as the detail shows it. */
data class CardDetail(
    val id: Long,
    val title: String,
    val description: String,
    val pendingSync: Boolean,
    val conflicts: List<TextConflict>
)

/**
 * One card being read and edited (T14). Edits are saved automatically shortly after typing
 * stops, and right away when leaving the card; they work offline and sync later.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    session: AccountSession,
    database: UltimateDeckDatabase,
    private val actions: CardActions
) : ViewModel() {
    private val details = database.cardDetailDao()
    private val cardId = MutableStateFlow<Long?>(null)
    private val titles =
        MutableSharedFlow<Pair<Long, String>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val descriptions =
        MutableSharedFlow<Pair<Long, String>>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val mutableRevision = MutableStateFlow(0)

    /** Changes when the shown texts were replaced (server version chosen): editors reload. */
    val revision: StateFlow<Int> = mutableRevision.asStateFlow()

    val card: StateFlow<CardDetail?> = combine(session.activeAccount, cardId) { account, id ->
        account?.id to id
    }.flatMapLatest { (accountId, id) ->
        if (accountId == null || id == null) {
            flowOf(null)
        } else {
            combine(details.observeCard(accountId, id), details.observeSnapshot(accountId, id)) {
                    card,
                    snapshot
                ->
                card?.let {
                    CardDetail(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        pendingSync = it.dirtyFields != 0,
                        conflicts = textConflicts(it, snapshot)
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            titles.debounce(SAVE_DELAY_MS).collect { (id, title) -> actions.editTitle(id, title) }
        }
        viewModelScope.launch {
            descriptions.debounce(SAVE_DELAY_MS).collect { (id, text) ->
                actions.editDescription(id, text)
            }
        }
    }

    fun open(id: Long) {
        cardId.value = id
    }

    fun onTitleChange(title: String) {
        cardId.value?.let { titles.tryEmit(it to title) }
    }

    fun onDescriptionChange(description: String) {
        cardId.value?.let { descriptions.tryEmit(it to description) }
    }

    /** Saves what is still waiting for the pause, e.g. when leaving the card. */
    fun saveNow(title: String, description: String) {
        val id = cardId.value ?: return
        viewModelScope.launch {
            actions.editTitle(id, title)
            actions.editDescription(id, description)
        }
    }

    fun resolve(field: CardField, keepMine: Boolean) {
        val id = cardId.value ?: return
        viewModelScope.launch {
            actions.resolveConflict(id, field, keepMine)
            if (!keepMine) mutableRevision.value++
        }
    }
}
