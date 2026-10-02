// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.remote.dto.AttachmentDto
import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.dto.UserDto
import com.qtekfun.ultimatedeck.data.remote.mapper.DeckDates
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.data.remote.mapper.toSnapshot
import com.qtekfun.ultimatedeck.data.remote.mapper.toUpdateRequest
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeckMappersTest {
    private val boards = DeckJson.decodeFromString<List<BoardDto>>(ApiFixtures.read("boards.json"))
    private val stacks = DeckJson.decodeFromString<List<StackDto>>(ApiFixtures.read("stacks.json"))
    private val card = stacks.first().cards.single()

    @Test
    fun `maps a board with its owner and dates`() {
        val board = boards.first().toEntity(accountId = 7)

        assertEquals(7L to 10L, board.accountId to board.id)
        assertEquals("admin", board.ownerUid)
        assertEquals(Instant.ofEpochSecond(1541426139), board.lastModified)
        assertNull(board.deletedAt)
        assertEquals("a1b2c3", board.etag)
        assertEquals(Instant.ofEpochSecond(1541426200), boards[1].toEntity(7).deletedAt)
    }

    @Test
    fun `maps labels, users and columns`() {
        assertEquals(10L, boards.first().labels.first().toEntity(7, boardId = 10).boardId)
        assertEquals("Administrator", boards.first().users.first().toEntity(7).displayName)
        assertEquals("ana", UserDto("ana").toEntity(7).displayName)
        val stack = stacks.first().toEntity(7)
        assertEquals(10L to 999, stack.boardId to stack.order)
    }

    @Test
    fun `maps a card to the local row with nothing dirty`() {
        val entity = card.toEntity(accountId = 7, boardId = 10)

        assertEquals(4L, entity.stackId)
        assertEquals("Some **markdown**", entity.description)
        assertEquals(Instant.parse("2019-12-24T19:29:30Z"), entity.dueDate)
        assertNull(entity.done)
        assertEquals("admin", entity.ownerUid)
        assertEquals(0, entity.dirtyFields)
    }

    @Test
    fun `maps a card to its server snapshot with labels and assignees`() {
        val snapshot = card.toSnapshot(accountId = 7)

        assertEquals(listOf(37L), snapshot.labelIds)
        assertEquals(listOf("admin"), snapshot.assigneeUids)
        assertEquals("bdb10fa2d2aeda092a2b6b469454dc90", snapshot.etag)
    }

    @Test
    fun `fills missing card fields with neutral values`() {
        val bare = CardDto(id = 1, title = "T", stackId = 2)

        assertEquals("", bare.toEntity(7, 1).description)
        assertEquals(emptyList<Long>(), bare.toSnapshot(7).labelIds)
    }

    @Test
    fun `maps attachments with their file metadata`() {
        val attachment = DeckJson.decodeFromString<List<AttachmentDto>>(
            ApiFixtures.read("attachments.json")
        )
            .single()
            .toEntity(7)

        assertEquals("screenshot.png", attachment.fileName)
        assertEquals("image/png" to 2048L, attachment.mimeType to attachment.size)
        assertEquals(UploadState.DONE, attachment.uploadState)
        assertEquals(0L, AttachmentDto(1, 2, "file", "a").toEntity(7).size)
    }

    @Test
    fun `builds the update request from the local card`() {
        val entity = card.toEntity(
            7,
            10
        ).copy(ownerUid = null, done = Instant.parse("2026-10-04T10:00:00Z"))

        val request = entity.toUpdateRequest(fallbackOwner = "ana")

        assertEquals("ana", request.owner)
        assertEquals("2019-12-24T19:29:30+00:00", request.duedate)
        assertEquals("2026-10-04T10:00:00+00:00", request.done)
    }

    @Test
    fun `reads Deck dates and treats zero, blank and garbage as not set`() {
        assertNull(DeckDates.fromEpochSeconds(0))
        assertNull(DeckDates.fromEpochSeconds(null))
        assertNull(DeckDates.fromIso(""))
        assertNull(DeckDates.fromIso("not a date"))
        assertNull(DeckDates.toIso(null))
        assertEquals(
            Instant.parse("2026-10-04T08:00:00Z"),
            DeckDates.fromIso("2026-10-04T10:00:00+02:00")
        )
    }
}
