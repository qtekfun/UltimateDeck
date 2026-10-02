// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Creating, archiving and deleting cards (RF-06). Each change is saved in Room first, so it
 * shows at once and works offline, then queued for the server and a sync is requested.
 */
class CardActions @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val queue: OperationQueue,
    private val clock: Clock,
    private val scheduler: SyncScheduler
) {
    private val cards = database.cardDao()
    private val edits = database.cardLocalEditDao()
    private val localIds = database.localIdDao()

    /** Adds a card at the end of a column; a blank title creates nothing. */
    suspend fun create(boardId: Long, stackId: Long, title: String) {
        val name = title.trim()
        val accountId = accountId() ?: return
        if (name.isEmpty()) return
        val id = localIds.nextId(accountId, EntityType.CARD)
        val order = (cards.maxOrder(accountId, stackId) ?: -1) + 1
        cards.upsert(
            listOf(
                CardEntity(
                    accountId = accountId,
                    id = id,
                    boardId = boardId,
                    stackId = stackId,
                    title = name,
                    order = order,
                    dirtyFields = CardField.TITLE.bit,
                    localModifiedAt = clock.instant()
                )
            )
        )
        queue.enqueue(accountId, id, QueuedOperation.CreateCard(boardId, stackId, name, order))
        scheduler.requestSync()
    }

    /** Archives or restores a card; restoring right after archiving cancels both. */
    suspend fun setArchived(cardId: Long, archived: Boolean) {
        val accountId = accountId() ?: return
        val card = cards.get(accountId, cardId) ?: return
        edits.updateArchived(accountId, cardId, archived, clock.instant())
        queue.enqueue(
            accountId,
            cardId,
            QueuedOperation.ArchiveCard(card.boardId, card.stackId, archived)
        )
        scheduler.requestSync()
    }

    /** Deletes a card: one never sent leaves no trace, others hide until the server deletes them. */
    suspend fun delete(cardId: Long) {
        val accountId = accountId() ?: return
        val card = cards.get(accountId, cardId) ?: return
        queue.enqueue(accountId, cardId, QueuedOperation.DeleteCard(card.boardId, card.stackId))
        if (cardId < 0) {
            cards.delete(accountId, cardId)
        } else {
            edits.markDeleted(accountId, cardId, clock.instant())
            scheduler.requestSync()
        }
    }

    private suspend fun accountId() = session.activeAccount.first()?.id
}
