// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.queue

import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A local change waiting for the server, stored as the JSON payload of a pending operation.
 * Every operation except [CreateCard] carries the final absolute state (position, labels,
 * assignees...), so sending it twice is harmless: the queue can always retry.
 */
@Serializable
sealed interface QueuedOperation {
    val type: OperationType
    val entityType: EntityType

    /** Not idempotent on Deck: retrying after a lost response could duplicate the card (T09). */
    @Serializable
    @SerialName("create_card")
    data class CreateCard(val boardId: Long, val stackId: Long, val title: String, val order: Int) :
        QueuedOperation {
        override val type get() = OperationType.CREATE
        override val entityType get() = EntityType.CARD
    }

    /** Sends the card's current local values, read when the operation runs. */
    @Serializable
    @SerialName("update_card")
    data class UpdateCard(val boardId: Long, val stackId: Long) : QueuedOperation {
        override val type get() = OperationType.UPDATE
        override val entityType get() = EntityType.CARD
    }

    @Serializable
    @SerialName("move_card")
    data class MoveCard(
        val boardId: Long,
        val fromStackId: Long,
        val stackId: Long,
        val order: Int
    ) : QueuedOperation {
        override val type get() = OperationType.MOVE
        override val entityType get() = EntityType.CARD
    }

    @Serializable
    @SerialName("archive_card")
    data class ArchiveCard(val boardId: Long, val stackId: Long, val archived: Boolean) :
        QueuedOperation {
        override val type get() = OperationType.ARCHIVE
        override val entityType get() = EntityType.CARD
    }

    @Serializable
    @SerialName("delete_card")
    data class DeleteCard(val boardId: Long, val stackId: Long) : QueuedOperation {
        override val type get() = OperationType.DELETE
        override val entityType get() = EntityType.CARD
    }

    @Serializable
    @SerialName("set_labels")
    data class SetLabels(val boardId: Long, val stackId: Long, val labelIds: List<Long>) :
        QueuedOperation {
        override val type get() = OperationType.SET_LABELS
        override val entityType get() = EntityType.CARD
    }

    @Serializable
    @SerialName("set_assignees")
    data class SetAssignees(val boardId: Long, val stackId: Long, val uids: List<String>) :
        QueuedOperation {
        override val type get() = OperationType.SET_ASSIGNEES
        override val entityType get() = EntityType.CARD
    }

    /** The attachment is the entity; its file and card come from the attachment row. */
    @Serializable
    @SerialName("upload_attachment")
    data class UploadAttachment(val boardId: Long, val stackId: Long, val cardId: Long) :
        QueuedOperation {
        override val type get() = OperationType.UPLOAD
        override val entityType get() = EntityType.ATTACHMENT
    }

    companion object {
        /** Payload JSON; the class name goes in "op" to keep it apart from the fields. */
        val json: Json = Json {
            classDiscriminator = "op"
            ignoreUnknownKeys = true
        }

        fun encode(operation: QueuedOperation): String =
            json.encodeToString(serializer(), operation)

        fun decode(payload: String): QueuedOperation = json.decodeFromString(serializer(), payload)
    }
}
