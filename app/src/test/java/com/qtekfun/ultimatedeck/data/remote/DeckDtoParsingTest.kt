// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.AttachmentDto
import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.dto.UpdateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserDto
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeckDtoParsingTest {

    @Test
    fun `parses boards with labels, users, owner object and ETag`() {
        val boards = DeckJson.decodeFromString<List<BoardDto>>(ApiFixtures.read("boards.json"))

        val board = boards.first()
        assertEquals(10L, board.id)
        assertEquals("ff0000", board.color)
        assertEquals(UserDto("admin", "Administrator"), board.owner)
        assertEquals(listOf(37L, 38L), board.labels.map { it.id })
        assertEquals(listOf("admin"), board.users.map { it.uid })
        assertEquals(1541426139L, board.lastModified)
        assertEquals("a1b2c3", board.etag)
    }

    @Test
    fun `tolerates an owner string, missing lists and unknown fields`() {
        val board = DeckJson.decodeFromString<List<BoardDto>>(ApiFixtures.read("boards.json"))[1]

        assertEquals(UserDto("admin"), board.owner)
        assertTrue(board.archived)
        assertEquals(1541426200L, board.deletedAt)
        assertTrue(board.labels.isEmpty())
        assertNull(board.etag)
    }

    @Test
    fun `parses stacks with their cards, labels and assignees`() {
        val stacks = DeckJson.decodeFromString<List<StackDto>>(ApiFixtures.read("stacks.json"))

        val card = stacks.first().cards.single()
        assertEquals(81L, card.id)
        assertEquals("Some **markdown**", card.description)
        assertEquals("2019-12-24T19:29:30+00:00", card.duedate)
        assertNull(card.done)
        assertEquals(listOf(37L), card.labels?.map { it.id })
        assertEquals(listOf("admin"), card.assignedUsers?.map { it.participant.uid })
        assertEquals(2, card.attachmentCount)
        assertEquals("bdb10fa2d2aeda092a2b6b469454dc90", card.etag)
    }

    @Test
    fun `treats a null card list as empty`() {
        val stacks = DeckJson.decodeFromString<List<StackDto>>(ApiFixtures.read("stacks.json"))

        assertTrue(stacks[1].cards.isEmpty())
    }

    @Test
    fun `parses a created card with nulls and an owner string`() {
        val card = DeckJson.decodeFromString<CardDto>(ApiFixtures.read("card_created.json"))

        assertEquals(10L, card.id)
        assertNull(card.description)
        assertNull(card.labels)
        assertEquals(UserDto("admin"), card.owner)
        assertEquals(999, card.order)
    }

    @Test
    fun `parses v1 point 1 attachments with their file metadata`() {
        val attachment = DeckJson.decodeFromString<List<AttachmentDto>>(
            ApiFixtures.read("attachments.json")
        ).single()

        assertEquals("file", attachment.type)
        assertEquals("screenshot.png", attachment.data)
        assertEquals(2048L, attachment.extendedData?.filesize)
        assertEquals("image/png", attachment.extendedData?.mimetype)
    }

    @Test
    fun `omits null optional fields when encoding requests`() {
        val json = DeckJson.encodeToString(
            UpdateCardRequest(title = "T", owner = "admin", order = 1, description = "")
        )

        val fields = DeckJson.parseToJsonElement(json).jsonObject.keys
        assertEquals(setOf("title", "owner", "order", "description", "type", "archived"), fields)
    }
}
