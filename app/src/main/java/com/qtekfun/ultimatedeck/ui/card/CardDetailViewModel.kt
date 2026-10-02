// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.domain.card.CardActions
import com.qtekfun.ultimatedeck.domain.card.CardMetadataActions
import com.qtekfun.ultimatedeck.domain.card.textConflicts
import com.qtekfun.ultimatedeck.sync.conflict.TextConflict
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
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
    val conflicts: List<TextConflict>,
    val dueDate: Instant? = null,
    val labels: List<LabelChoice> = emptyList(),
    val members: List<MemberChoice> = emptyList()
)

/** A label of the board; [selected] when the card has it. */
data class LabelChoice(val id: Long, val title: String, val color: String, val selected: Boolean)

/** A user who can be assigned: board members, plus anyone already assigned. */
data class MemberChoice(val uid: String, val name: String, val selected: Boolean)

/**
 * One card being read and edited (T14). Edits are saved automatically shortly after typing
 * stops, and right away when leaving the card; they work offline and sync later.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    session: AccountSession,
    database: UltimateDeckDatabase,
    private val actions: CardActions,
    private val metadata: CardMetadataActions,
    private val scheduler: SyncScheduler
) : ViewModel() {
    private val details = database.cardDetailDao()
    private val labels = database.labelDao()
    private val users = database.userDao()
    private val members = database.boardMemberDao()
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
        if (accountId == null || id == null) flowOf(null) else observeDetail(accountId, id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun observeDetail(accountId: Long, id: Long): Flow<CardDetail?> =
        details.observeCard(accountId, id).flatMapLatest { card ->
            if (card == null) {
                flowOf(null)
            } else {
                combine(
                    details.observeSnapshot(accountId, id),
                    labels.observeForBoard(accountId, card.boardId),
                    labels.observeCardLabels(accountId, card.boardId),
                    members.observeMembers(accountId, card.boardId),
                    users.observeCardAssignees(accountId, card.boardId)
                ) { snapshot, boardLabels, cardLabels, boardMembers, assignees ->
                    val chosen = cardLabels.filter { it.cardId == id }.map { it.labelId }.toSet()
                    val assigned = assignees.filter { it.cardId == id }
                    val assignedUids = assigned.map { it.uid }.toSet()
                    val people = boardMembers.map { it.uid to it.displayName } +
                        assigned.filter { a -> boardMembers.none { it.uid == a.uid } }
                            .map { it.uid to it.displayName }
                    CardDetail(
                        id = card.id,
                        title = card.title,
                        description = card.description,
                        pendingSync = card.dirtyFields != 0,
                        conflicts = textConflicts(card, snapshot),
                        dueDate = card.dueDate,
                        labels = boardLabels.map {
                            LabelChoice(it.id, it.title, it.color, it.id in chosen)
                        },
                        members = people.map { (uid, name) ->
                            MemberChoice(uid, name, uid in assignedUids)
                        }
                    )
                }
            }
        }

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
            scheduler.requestSync()
        }
    }

    fun setDueDate(dueDate: Instant?) = launchFor { metadata.setDueDate(it, dueDate) }

    fun setLabels(labelIds: Set<Long>) = launchFor { metadata.setLabels(it, labelIds) }

    fun setAssignees(uids: Set<String>) = launchFor { metadata.setAssignees(it, uids) }

    private fun launchFor(change: suspend (cardId: Long) -> Unit) {
        val id = cardId.value ?: return
        viewModelScope.launch { change(id) }
    }

    fun resolve(field: CardField, keepMine: Boolean) {
        val id = cardId.value ?: return
        viewModelScope.launch {
            actions.resolveConflict(id, field, keepMine)
            if (!keepMine) mutableRevision.value++
        }
    }
}
