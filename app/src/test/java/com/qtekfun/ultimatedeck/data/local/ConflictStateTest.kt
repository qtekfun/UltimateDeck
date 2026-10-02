// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.model.CardField
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConflictStateTest {
    private val db = inMemoryDatabase()
    private val edits = db.cardLocalEditDao()
    private val snapshots = db.cardSnapshotDao()
    private val t0 = Instant.parse("2026-10-01T10:00:00Z")
    private val t1 = t0.plusSeconds(60)

    @AfterEach
    fun close() = db.close()

    private suspend fun dirtyFields(accountId: Long, id: Long = 100) =
        CardField.fromMask(db.cardDao().get(accountId, id)!!.dirtyFields)

    @Test
    fun `each local edit changes its value and marks only its own field`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        db.stackDao().upsert(listOf(Fixtures.stack(accountId, 11)))

        edits.updateTitle(accountId, 100, "New title", t0)
        assertEquals(setOf(CardField.TITLE), dirtyFields(accountId))

        edits.updateDescription(accountId, 100, "Text", t0)
        edits.updateDueDate(accountId, 100, Instant.EPOCH, t0)
        edits.updatePosition(accountId, 100, stackId = 11, order = 3, modifiedAt = t0)
        edits.updateArchived(accountId, 100, true, t1)

        val card = db.cardDao().get(accountId, 100)!!
        assertEquals("New title", card.title)
        assertEquals("Text", card.description)
        assertEquals(Instant.EPOCH, card.dueDate)
        assertEquals(11L to 3, card.stackId to card.order)
        assertEquals(true, card.archived)
        assertEquals(t1, card.localModifiedAt)
        assertEquals(
            setOf(
                CardField.TITLE,
                CardField.DESCRIPTION,
                CardField.DUE_DATE,
                CardField.POSITION,
                CardField.ARCHIVED
            ),
            dirtyFields(accountId)
        )
    }

    @Test
    fun `marks and clears arbitrary fields without touching the others`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        edits.markDirty(
            accountId,
            100,
            CardField.maskOf(listOf(CardField.LABELS, CardField.ASSIGNEES, CardField.TITLE)),
            t0
        )

        edits.clearDirty(
            accountId,
            100,
            CardField.maskOf(listOf(CardField.LABELS, CardField.TITLE))
        )

        assertEquals(setOf(CardField.ASSIGNEES), dirtyFields(accountId))
    }

    @Test
    fun `lists the cards of a board with unsynced changes`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100, 101)

        edits.observeDirtyCardIds(accountId, 1).test {
            assertEquals(emptyList<Long>(), awaitItem())
            edits.updateTitle(accountId, 101, "Changed", t0)
            assertEquals(listOf(101L), awaitItem())
            edits.clearDirty(accountId, 101, CardField.TITLE.bit)
            assertEquals(emptyList<Long>(), awaitItem())
        }
    }

    @Test
    fun `stores the server snapshot with its lists and replaces it`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        val snapshot = CardServerSnapshotEntity(
            accountId = accountId,
            cardId = 100,
            title = "Server title",
            description = "",
            stackId = 10,
            order = 0,
            archived = false,
            dueDate = null,
            done = null,
            labelIds = listOf(3, 1),
            assigneeUids = listOf("ana", "user with spaces", "a,b"),
            lastModified = Instant.parse("2026-10-01T00:00:00Z"),
            etag = "abc"
        )
        snapshots.put(snapshot)
        assertEquals(snapshot, snapshots.get(accountId, 100))

        snapshots.put(
            snapshot.copy(title = "Newer", labelIds = emptyList(), assigneeUids = emptyList())
        )
        assertEquals(
            snapshot.copy(title = "Newer", labelIds = emptyList(), assigneeUids = emptyList()),
            snapshots.get(accountId, 100)
        )
    }

    @Test
    fun `deleting the card deletes its snapshot`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        snapshots.put(Fixtures.snapshot(accountId))

        db.cardDao().delete(accountId, 100)

        assertNull(snapshots.get(accountId, 100))
    }

    @Test
    fun `field masks round trip`() {
        val fields = setOf(CardField.DONE, CardField.POSITION)

        assertEquals(fields, CardField.fromMask(CardField.maskOf(fields)))
        assertEquals(emptySet<CardField>(), CardField.fromMask(0))
    }
}
