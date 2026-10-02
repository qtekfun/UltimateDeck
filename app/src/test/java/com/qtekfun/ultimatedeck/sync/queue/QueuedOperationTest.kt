// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.queue

import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.local.model.OperationType
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class QueuedOperationTest {

    @ParameterizedTest
    @MethodSource("operations")
    fun `round trips through its JSON payload`(operation: QueuedOperation) {
        val payload = QueuedOperation.encode(operation)

        assertEquals(operation, QueuedOperation.decode(payload))
        assertTrue(payload.contains("\"op\":"))
    }

    @ParameterizedTest
    @MethodSource("kinds")
    fun `knows its operation and entity type`(
        operation: QueuedOperation,
        type: OperationType,
        entity: EntityType
    ) {
        assertEquals(type to entity, operation.type to operation.entityType)
    }

    @ParameterizedTest
    @MethodSource("operations")
    fun `rejects a payload missing required fields`(operation: QueuedOperation) {
        val onlyDiscriminator = QueuedOperation.encode(operation).substringBefore(",") + "}"

        assertThrows<SerializationException> { QueuedOperation.decode(onlyDiscriminator) }
    }

    companion object {
        private val all = listOf(
            QueuedOperation.CreateCard(1, 2, "New", 3) to OperationType.CREATE,
            QueuedOperation.UpdateCard(1, 2) to OperationType.UPDATE,
            QueuedOperation.MoveCard(1, 2, 4, 0) to OperationType.MOVE,
            QueuedOperation.ArchiveCard(1, 2, true) to OperationType.ARCHIVE,
            QueuedOperation.DeleteCard(1, 2) to OperationType.DELETE,
            QueuedOperation.SetLabels(1, 2, listOf(7, 8)) to OperationType.SET_LABELS,
            QueuedOperation.SetAssignees(1, 2, listOf("ana")) to OperationType.SET_ASSIGNEES,
            QueuedOperation.UploadAttachment(1, 2, 3) to OperationType.UPLOAD,
            QueuedOperation.DeleteAttachment(1, 2, 3, "file") to OperationType.DELETE
        )

        @JvmStatic
        fun operations() = all.map { it.first }

        @JvmStatic
        fun kinds() = all.map { (operation, type) ->
            val attachment = operation is QueuedOperation.UploadAttachment ||
                operation is QueuedOperation.DeleteAttachment
            val entity = if (attachment) {
                EntityType.ATTACHMENT
            } else {
                EntityType.CARD
            }
            org.junit.jupiter.params.provider.Arguments.of(operation, type, entity)
        }
    }
}
