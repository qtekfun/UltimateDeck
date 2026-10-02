// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.remote

import androidx.compose.ui.graphics.Color
import com.qtekfun.ultimatedeck.data.remote.ApiFixtures
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.dto.AssignmentDto
import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.dto.UserDto
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemoteBoardsTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `lists only active boards, by title, with their colors`() {
        val boards = DeckJson.decodeFromString<List<BoardDto>>(ApiFixtures.read("boards.json")) +
            BoardDto(id = 12, title = "alpha", color = "31CC7C")

        val summaries = boards.toSummaries()

        assertEquals(listOf("alpha", "Board title"), summaries.map { it.title })
        assertEquals(Color(0xFF31CC7C), summaries.first().color)
    }

    @Test
    fun `falls back to the Nextcloud blue for unusable colors`() {
        assertEquals(Color(0xFF0082C9), deckColor(""))
        assertEquals(Color(0xFF0082C9), deckColor("zzzzzz"))
        assertEquals(Color(0xFFFF0000), deckColor("#ff0000"))
    }

    @Test
    fun `maps columns and cards in order, without archived or deleted ones`() {
        val stacks = listOf(
            StackDto(
                id = 2,
                title = "Second",
                boardId = 1,
                order = 1,
                cards = listOf(
                    CardDto(id = 21, title = "late", stackId = 2, order = 5),
                    CardDto(id = 20, title = "early", stackId = 2, order = 1),
                    CardDto(id = 22, title = "archived", stackId = 2, archived = true),
                    CardDto(id = 23, title = "deleted", stackId = 2, deletedAt = 100)
                )
            ),
            StackDto(id = 1, title = "First", boardId = 1, order = 0),
            StackDto(id = 3, title = "Deleted", boardId = 1, deletedAt = 100)
        )

        val columns = stacks.toColumns(madrid)

        assertEquals(listOf("First", "Second"), columns.map { it.title })
        assertEquals(listOf("early", "late"), columns[1].cards.map { it.title })
    }

    @Test
    fun `maps a card with labels, assignees, local due date and attachments`() {
        val card = DeckJson.decodeFromString<List<StackDto>>(
            ApiFixtures.read("stacks.json")
        ).first().cards.single()

        val mapped = card.toPrototypeCard(madrid)

        assertEquals(listOf("Finished"), mapped.labels.map { it.name })
        assertEquals(Color(0xFF31CC7C), mapped.labels.single().color)
        assertEquals(listOf("Administrator"), mapped.assignees)
        assertEquals(LocalDate.of(2019, 12, 24), mapped.dueDate)
        assertEquals(2, mapped.attachments)
        assertEquals("Some **markdown**", mapped.description)
    }

    @Test
    fun `counts the checklist from the description's task items`() {
        val card = CardDto(
            id = 1,
            title = "T",
            stackId = 1,
            description = "Plan\n- [x] one\n- [ ] two\n  - [X] nested\n",
            assignedUsers = listOf(AssignmentDto(UserDto("ana")))
        )

        val mapped = card.toPrototypeCard(madrid)

        assertEquals(2 to 3, mapped.checklistDone to mapped.checklistTotal)
        assertEquals(listOf("ana"), mapped.assignees)
    }

    @Test
    fun `maps API failures to what the user can do about them`() {
        assertEquals(RemoteError.UNAUTHORIZED, ApiResult.Unauthorized.toRemoteError())
        assertEquals(
            RemoteError.UNREACHABLE,
            ApiResult.NetworkError(ApiResult.NetworkError.Kind.TIMEOUT).toRemoteError()
        )
        assertEquals(RemoteError.OTHER, ApiResult.HttpError(500).toRemoteError())
        assertEquals(RemoteError.OTHER, ApiResult.ParseError.toRemoteError())
    }
}
