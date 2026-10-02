// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.conflict

import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import java.time.Instant

/** The editable values of a card, the same shape for local, last known and server versions. */
data class CardState(
    val title: String,
    val description: String,
    val dueDate: Instant?,
    val stackId: Long,
    val order: Int,
    val archived: Boolean,
    val done: Instant?,
    val labelIds: Set<Long>,
    val assigneeUids: Set<String>
) {
    companion object {
        fun of(card: CardEntity, labelIds: Set<Long>, assigneeUids: Set<String>) = CardState(
            title = card.title,
            description = card.description,
            dueDate = card.dueDate,
            stackId = card.stackId,
            order = card.order,
            archived = card.archived,
            done = card.done,
            labelIds = labelIds,
            assigneeUids = assigneeUids
        )

        fun of(snapshot: CardServerSnapshotEntity) = CardState(
            title = snapshot.title,
            description = snapshot.description,
            dueDate = snapshot.dueDate,
            stackId = snapshot.stackId,
            order = snapshot.order,
            archived = snapshot.archived,
            done = snapshot.done,
            labelIds = snapshot.labelIds.toSet(),
            assigneeUids = snapshot.assigneeUids.toSet()
        )
    }
}

/** The local card: its values, which fields changed locally and when the last change was. */
data class LocalCard(val state: CardState, val dirty: Set<CardField>, val modifiedAt: Instant?) {
    companion object {
        fun of(card: CardEntity, labelIds: Set<Long>, assigneeUids: Set<String>) = LocalCard(
            state = CardState.of(card, labelIds, assigneeUids),
            dirty = CardField.fromMask(card.dirtyFields),
            modifiedAt = card.localModifiedAt
        )
    }
}

/** The card as the server returns it now. */
data class ServerCard(val state: CardState, val lastModified: Instant?) {
    companion object {
        fun of(card: CardDto) = ServerCard(
            state = CardState(
                title = card.title,
                description = card.description.orEmpty(),
                dueDate = DeckDates.fromIso(card.duedate),
                stackId = card.stackId,
                order = card.order,
                archived = card.archived,
                done = DeckDates.fromIso(card.done),
                labelIds = card.labels.orEmpty().map { it.id }.toSet(),
                assigneeUids = card.assignedUsers.orEmpty().map { it.participant.uid }.toSet()
            ),
            lastModified = DeckDates.fromEpochSeconds(card.lastModified)
        )
    }
}
