// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
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
    private val database: UltimateDeckDatabase,
    private val queue: OperationQueue,
    private val clock: Clock,
    private val scheduler: SyncScheduler
) {
    private val cards = database.cardDao()
    private val edits = database.cardLocalEditDao()
    private val localIds = database.localIdDao()
    private val snapshots = database.cardSnapshotDao()

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

    /**
     * Moves a card to [toStackId]; [columnOrder] is that column's cards in their new order. The
     * other cards are renumbered as Deck does on the server, without counting as changes.
     */
    suspend fun move(cardId: Long, toStackId: Long, columnOrder: List<Long>) {
        val accountId = accountId() ?: return
        val card = cards.get(accountId, cardId)
        val index = columnOrder.indexOf(cardId)
        if (card == null || index < 0) return
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                columnOrder.forEachIndexed { order, id ->
                    if (id != cardId) edits.setOrder(accountId, id, order)
                }
                edits.updatePosition(accountId, cardId, toStackId, index, clock.instant())
            }
        }
        queue.enqueue(
            accountId,
            cardId,
            QueuedOperation.MoveCard(card.boardId, card.stackId, toStackId, index)
        )
        scheduler.requestSync()
    }

    /** Saves a new title; a blank or unchanged one is ignored. */
    suspend fun editTitle(cardId: Long, title: String) {
        val name = title.trim()
        edit(cardId) { accountId, card ->
            if (name.isEmpty() || name == card.title) return@edit false
            edits.updateTitle(accountId, cardId, name, clock.instant())
            true
        }
    }

    /** Saves a new description (Markdown); an unchanged one is ignored. */
    suspend fun editDescription(cardId: Long, description: String) {
        edit(cardId) { accountId, card ->
            if (description == card.description) return@edit false
            edits.updateDescription(accountId, cardId, description, clock.instant())
            true
        }
    }

    /**
     * Ends a title/description conflict (SPEC §5). Keeping mine sends it on the next sync; using
     * the server's replaces mine with the last version read from the server.
     */
    suspend fun resolveConflict(cardId: Long, field: CardField, keepMine: Boolean) {
        val accountId = accountId() ?: return
        val card = cards.get(accountId, cardId)
        val server = snapshots.get(accountId, cardId)
        if (card == null || server == null) return
        val unresolved = card.conflictFields and field.bit.inv()
        if (keepMine) {
            cards.update(listOf(card.copy(conflictFields = unresolved)))
            queueUpdate(accountId, card)
            scheduler.requestSync()
        } else {
            val theirs = card.copy(
                title = if (field == CardField.TITLE) server.title else card.title,
                description = if (field == CardField.DESCRIPTION) {
                    server.description
                } else {
                    card.description
                },
                conflictFields = unresolved,
                dirtyFields = card.dirtyFields and field.bit.inv()
            )
            cards.update(listOf(theirs))
        }
    }

    /**
     * Runs [change] on an existing card; when it saved something, the card is queued to send.
     * No sync is requested: typing would start one per pause (the card detail syncs when it is left).
     */
    private suspend fun edit(
        cardId: Long,
        change: suspend (accountId: Long, card: CardEntity) -> Boolean
    ) {
        val accountId = accountId() ?: return
        val card = cards.get(accountId, cardId) ?: return
        if (change(accountId, card)) queueUpdate(accountId, card)
    }

    private suspend fun queueUpdate(accountId: Long, card: CardEntity) {
        queue.enqueue(accountId, card.id, QueuedOperation.UpdateCard(card.boardId, card.stackId))
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
