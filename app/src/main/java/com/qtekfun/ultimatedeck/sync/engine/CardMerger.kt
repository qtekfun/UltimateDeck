// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.data.remote.mapper.toSnapshot
import com.qtekfun.ultimatedeck.sync.conflict.CardState
import com.qtekfun.ultimatedeck.sync.conflict.ConflictResolver
import com.qtekfun.ultimatedeck.sync.conflict.LocalCard
import com.qtekfun.ultimatedeck.sync.conflict.Resolution
import com.qtekfun.ultimatedeck.sync.conflict.ServerCard
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.time.Instant

/** Writes pulled cards into Room, resolving those changed here with [ConflictResolver]. */
internal class CardMerger(database: UltimateDeckDatabase, private val queue: OperationQueue) {
    private val cards = database.cardDao()
    private val labels = database.labelDao()
    private val users = database.userDao()
    private val snapshots = database.cardSnapshotDao()
    private val pending = database.pendingOperationDao()

    /** A card new to this device: stored as the server has it. */
    suspend fun insert(accountId: Long, boardId: Long, dto: CardDto, boardLabels: Set<Long>) =
        write(accountId, boardId, dto, ServerCard.of(dto).state, emptySet(), null, boardLabels)

    /** Resolves a card known here against the server version, or its absence. */
    suspend fun settle(
        accountId: Long,
        boardId: Long,
        card: CardEntity,
        dto: CardDto?,
        boardLabels: Set<Long>
    ) {
        // Deleted here: kept hidden while the server still has it, removed once it is gone.
        if (card.deletedAt != null) {
            if (dto == null) cards.delete(accountId, card.id)
            return
        }
        val local = LocalCard.of(
            card,
            labels.labelIdsOfCard(accountId, card.id).toSet(),
            users.assigneeUidsOfCard(accountId, card.id).toSet()
        )
        val base = snapshots.get(accountId, card.id)?.let(CardState::of)
        when (val resolution = ConflictResolver.resolve(local, base, dto?.let(ServerCard::of))) {
            is Resolution.Merged -> {
                val server = requireNotNull(dto)
                write(
                    accountId,
                    boardId,
                    server,
                    resolution.state,
                    resolution.toSend + resolution.conflicts.map { it.field },
                    card.localModifiedAt,
                    boardLabels
                )
                resend(accountId, boardId, card.id, server.stackId, resolution)
            }

            is Resolution.DeletedOnServer -> cards.markDeletedOnServer(accountId, card.id)

            Resolution.RemoveLocally -> cards.delete(accountId, card.id)
        }
    }

    /**
     * Local changes that won but are no longer queued (e.g. the server ignored them) are queued
     * again, so they are not left marked as pending forever. Queued or failed ones are left alone.
     */
    private suspend fun resend(
        accountId: Long,
        boardId: Long,
        cardId: Long,
        serverStackId: Long,
        resolution: Resolution.Merged
    ) {
        if (resolution.toSend.isEmpty()) return
        if (pending.forEntity(accountId, EntityType.CARD, cardId).isNotEmpty()) return
        val state = resolution.state
        resolution.toSend
            .map { field -> field.operation(boardId, serverStackId, state) }
            .distinct()
            .forEach { queue.enqueue(accountId, cardId, it) }
    }

    /** Stores the card with [state]; fields in [pending] stay marked as changed here. */
    @Suppress("LongParameterList")
    private suspend fun write(
        accountId: Long,
        boardId: Long,
        dto: CardDto,
        state: CardState,
        pending: Set<CardField>,
        modifiedAt: Instant?,
        boardLabels: Set<Long>
    ) {
        val card = dto.toEntity(accountId, boardId).copy(
            title = state.title,
            description = state.description,
            dueDate = state.dueDate,
            stackId = state.stackId,
            order = state.order,
            archived = state.archived,
            done = state.done,
            dirtyFields = CardField.maskOf(pending),
            localModifiedAt = modifiedAt.takeIf { pending.isNotEmpty() }
        )
        cards.upsert(listOf(card))
        labels.setCardLabels(accountId, card.id, state.labelIds.filter { it in boardLabels })
        users.setAssignees(accountId, card.id, state.assigneeUids.toList())
        snapshots.put(dto.toSnapshot(accountId))
    }
}

/** The queued operation that sends this field of [state] to the server. */
private fun CardField.operation(boardId: Long, serverStackId: Long, state: CardState) =
    when (this) {
        CardField.TITLE, CardField.DESCRIPTION, CardField.DUE_DATE, CardField.DONE ->
            QueuedOperation.UpdateCard(boardId, state.stackId)

        CardField.POSITION -> QueuedOperation.MoveCard(
            boardId,
            serverStackId,
            state.stackId,
            state.order
        )

        CardField.ARCHIVED -> QueuedOperation.ArchiveCard(boardId, state.stackId, state.archived)

        CardField.LABELS -> QueuedOperation.SetLabels(
            boardId,
            state.stackId,
            state.labelIds.toList()
        )

        CardField.ASSIGNEES ->
            QueuedOperation.SetAssignees(boardId, state.stackId, state.assigneeUids.toList())
    }
