// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Due date, labels and assignees of a card (T16). Like other edits, saved in Room first and
 * queued; a change to the same value is ignored.
 */
class CardMetadataActions @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val queue: OperationQueue,
    private val clock: Clock,
    private val scheduler: SyncScheduler
) {
    private val cards = database.cardDao()
    private val edits = database.cardLocalEditDao()
    private val labels = database.labelDao()
    private val users = database.userDao()

    suspend fun setDueDate(cardId: Long, dueDate: Instant?) = change(cardId) { accountId, card ->
        if (card.dueDate == dueDate) return@change null
        edits.updateDueDate(accountId, cardId, dueDate, clock.instant())
        QueuedOperation.UpdateCard(card.boardId, card.stackId)
    }

    suspend fun setLabels(cardId: Long, labelIds: Set<Long>) = change(cardId) { accountId, card ->
        if (labels.labelIdsOfCard(accountId, cardId).toSet() == labelIds) return@change null
        labels.setCardLabels(accountId, cardId, labelIds.toList())
        edits.markDirty(accountId, cardId, CardField.LABELS.bit, clock.instant())
        QueuedOperation.SetLabels(card.boardId, card.stackId, labelIds.sorted())
    }

    suspend fun setAssignees(cardId: Long, uids: Set<String>) = change(cardId) { accountId, card ->
        if (users.assigneeUidsOfCard(accountId, cardId).toSet() == uids) return@change null
        users.setAssignees(accountId, cardId, uids.toList())
        edits.markDirty(accountId, cardId, CardField.ASSIGNEES.bit, clock.instant())
        QueuedOperation.SetAssignees(card.boardId, card.stackId, uids.sorted())
    }

    /** Applies [save] to an existing card and queues the operation it returns, if any. */
    private suspend fun change(
        cardId: Long,
        save: suspend (accountId: Long, card: CardEntity) -> QueuedOperation?
    ) {
        val accountId = session.activeAccount.first()?.id ?: return
        val operation = cards.get(accountId, cardId)?.let { save(accountId, it) } ?: return
        queue.enqueue(accountId, cardId, operation)
        scheduler.requestSync()
    }
}
