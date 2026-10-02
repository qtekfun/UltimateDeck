// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.data.remote.mapper.toSnapshot
import com.qtekfun.ultimatedeck.sync.conflict.CardState
import com.qtekfun.ultimatedeck.sync.conflict.ConflictResolver
import com.qtekfun.ultimatedeck.sync.conflict.LocalCard
import com.qtekfun.ultimatedeck.sync.conflict.Resolution
import com.qtekfun.ultimatedeck.sync.conflict.ServerCard
import java.time.Instant

/** Writes pulled cards into Room, resolving those changed here with [ConflictResolver]. */
internal class CardMerger(database: UltimateDeckDatabase) {
    private val cards = database.cardDao()
    private val labels = database.labelDao()
    private val users = database.userDao()
    private val snapshots = database.cardSnapshotDao()

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
            is Resolution.Merged -> write(
                accountId,
                boardId,
                requireNotNull(dto),
                resolution.state,
                resolution.toSend + resolution.conflicts.map { it.field },
                card.localModifiedAt,
                boardLabels
            )

            is Resolution.DeletedOnServer -> cards.markDeletedOnServer(accountId, card.id)

            Resolution.RemoveLocally -> cards.delete(accountId, card.id)
        }
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
