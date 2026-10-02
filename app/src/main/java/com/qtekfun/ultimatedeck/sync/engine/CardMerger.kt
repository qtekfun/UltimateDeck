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
    suspend fun insert(scope: BoardScope, dto: CardDto) =
        write(scope, dto, ServerCard.of(dto).state, LocalMarks.NONE)

    /** Resolves a card known here against the server version, or its absence. */
    suspend fun settle(scope: BoardScope, card: CardEntity, dto: CardDto?) {
        val accountId = scope.accountId
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
                // Conflicts wait for the user (T14): new ones, and older ones still unsolved.
                val serverState = ServerCard.of(server).state
                val conflicts = resolution.conflicts.map { it.field }.toSet() +
                    CardField.fromMask(card.conflictFields).filter {
                        it.textIn(local.state) != it.textIn(serverState)
                    }
                val toSend = resolution.toSend - conflicts
                val marks = LocalMarks(toSend + conflicts, conflicts, card.localModifiedAt)
                write(scope, server, resolution.state, marks)
                resend(scope, card.id, server.stackId, resolution.state, toSend)
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
        scope: BoardScope,
        cardId: Long,
        serverStackId: Long,
        state: CardState,
        toSend: Set<CardField>
    ) {
        if (toSend.isEmpty()) return
        val accountId = scope.accountId
        if (pending.forEntity(accountId, EntityType.CARD, cardId).isNotEmpty()) return
        toSend
            .map { field -> field.operation(scope.boardId, serverStackId, state) }
            .distinct()
            .forEach { queue.enqueue(accountId, cardId, it) }
    }

    /** Stores the card with [state]; fields in [pending] stay marked as changed here. */
    private suspend fun write(
        scope: BoardScope,
        dto: CardDto,
        state: CardState,
        marks: LocalMarks
    ) {
        val accountId = scope.accountId
        val card = dto.toEntity(accountId, scope.boardId).copy(
            title = state.title,
            description = state.description,
            dueDate = state.dueDate,
            stackId = state.stackId,
            order = state.order,
            archived = state.archived,
            done = state.done,
            dirtyFields = CardField.maskOf(marks.pending),
            localModifiedAt = marks.modifiedAt.takeIf { marks.pending.isNotEmpty() },
            conflictFields = CardField.maskOf(marks.conflicts)
        )
        cards.upsert(listOf(card))
        labels.setCardLabels(accountId, card.id, state.labelIds.filter { it in scope.labels })
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

/** What stays marked on a stored card: fields still to send, text conflicts, edit time. */
private data class LocalMarks(
    val pending: Set<CardField>,
    val conflicts: Set<CardField>,
    val modifiedAt: Instant?
) {
    companion object {
        val NONE = LocalMarks(emptySet(), emptySet(), null)
    }
}

/** The text of a title or description field; null for other fields. */
internal fun CardField.textIn(state: CardState): String? = when (this) {
    CardField.TITLE -> state.title
    CardField.DESCRIPTION -> state.description
    else -> null
}

/** The board being pulled: its account, id and the labels it has, which cards may use. */
internal data class BoardScope(val accountId: Long, val boardId: Long, val labels: Set<Long>)
