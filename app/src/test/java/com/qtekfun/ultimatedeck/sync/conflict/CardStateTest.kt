// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.conflict

import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.data.remote.dto.AssignmentDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.LabelDto
import com.qtekfun.ultimatedeck.data.remote.dto.UserDto
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardStateTest {
    private val due = Instant.parse("2026-10-04T10:00:00Z")
    private val state = CardState(
        title = "T",
        description = "D",
        dueDate = due,
        stackId = 2,
        order = 3,
        archived = true,
        done = null,
        labelIds = setOf(7, 8),
        assigneeUids = setOf("ana")
    )

    @Test
    fun `reads the local card with its edit marks and time`() {
        val card = CardEntity(
            accountId = 1,
            id = 10,
            boardId = 1,
            stackId = 2,
            title = "T",
            description = "D",
            order = 3,
            archived = true,
            dueDate = due,
            dirtyFields = CardField.maskOf(setOf(CardField.TITLE, CardField.LABELS)),
            localModifiedAt = due
        )

        val local = LocalCard.of(card, setOf(7, 8), setOf("ana"))

        assertEquals(LocalCard(state, setOf(CardField.TITLE, CardField.LABELS), due), local)
    }

    @Test
    fun `reads the last known server state`() {
        val snapshot = CardServerSnapshotEntity(
            accountId = 1,
            cardId = 10,
            title = "T",
            description = "D",
            stackId = 2,
            order = 3,
            archived = true,
            dueDate = due,
            done = null,
            labelIds = listOf(8, 7, 8),
            assigneeUids = listOf("ana"),
            lastModified = null,
            etag = null
        )

        assertEquals(state, CardState.of(snapshot))
    }

    @Test
    fun `reads the card the server returns`() {
        val dto = CardDto(
            id = 10,
            title = "T",
            stackId = 2,
            description = "D",
            order = 3,
            archived = true,
            duedate = "2026-10-04T12:00:00+02:00",
            labels = listOf(LabelDto(7, "a", "fff"), LabelDto(8, "b", "000")),
            assignedUsers = listOf(AssignmentDto(UserDto("ana"))),
            lastModified = 1_790_000_000
        )

        assertEquals(ServerCard(state, Instant.ofEpochSecond(1_790_000_000)), ServerCard.of(dto))
    }

    @Test
    fun `missing server lists and description are empty`() {
        val server = ServerCard.of(CardDto(id = 10, title = "T", stackId = 2))

        assertEquals("", server.state.description)
        assertEquals(emptySet<Long>(), server.state.labelIds)
        assertEquals(emptySet<String>(), server.state.assigneeUids)
        assertEquals(null, server.lastModified)
    }
}
