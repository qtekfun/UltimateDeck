// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.local.model.CardAssigneeRow
import com.qtekfun.ultimatedeck.data.local.model.CardLabelRow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LabelUserAttachmentDaoTest {
    private val db = inMemoryDatabase()
    private val labels = db.labelDao()
    private val users = db.userDao()
    private val attachments = db.attachmentDao()

    @AfterEach
    fun close() = db.close()

    private fun label(accountId: Long, id: Long, title: String) =
        LabelEntity(accountId = accountId, id = id, boardId = 1, title = title, color = "ff0000")

    private fun attachment(accountId: Long, id: Long, state: UploadState = UploadState.DONE) =
        AttachmentEntity(
            accountId,
            id,
            cardId = 100,
            fileName = "f$id.png",
            mimeType = "image/png",
            size = 1,
            uploadState = state
        )

    @Test
    fun `lists the labels of every card of a board`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100, 101)
        labels.upsert(listOf(label(accountId, 1, "Bug"), label(accountId, 2, "Feature")))
        labels.setCardLabels(accountId, 100, listOf(2, 1))
        labels.setCardLabels(accountId, 101, listOf(2))

        labels.observeCardLabels(accountId, 1).test {
            assertEquals(
                listOf(
                    CardLabelRow(100, 1, "Bug", "ff0000"),
                    CardLabelRow(100, 2, "Feature", "ff0000"),
                    CardLabelRow(101, 2, "Feature", "ff0000")
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `setting the labels of a card replaces the previous ones`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        labels.upsert(listOf(label(accountId, 1, "Bug"), label(accountId, 2, "Feature")))
        labels.setCardLabels(accountId, 100, listOf(1, 2))

        labels.setCardLabels(accountId, 100, listOf(2))

        labels.observeCardLabels(accountId, 1).test {
            assertEquals(listOf(2L), awaitItem().map { it.labelId })
        }
    }

    @Test
    fun `updating a label keeps it on its cards and deleting a card removes its links`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100, 101)
        labels.upsert(listOf(label(accountId, 1, "Bug")))
        labels.setCardLabels(accountId, 100, listOf(1))
        labels.setCardLabels(accountId, 101, listOf(1))

        labels.upsert(listOf(label(accountId, 1, "Defect")))
        db.cardDao().delete(accountId, 101)

        labels.observeCardLabels(accountId, 1).test {
            assertEquals(listOf(CardLabelRow(100, 1, "Defect", "ff0000")), awaitItem())
        }
        labels.observeForBoard(accountId, 1).test {
            assertEquals(listOf("Defect"), awaitItem().map { it.title })
        }
    }

    @Test
    fun `lists assignees per card and replaces them`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100, 101)
        users.upsert(
            listOf(
                DeckUserEntity(accountId, "ana", "Ana"),
                DeckUserEntity(accountId, "luis", "Luis")
            )
        )
        users.setAssignees(accountId, 100, listOf("luis", "ana"))
        users.setAssignees(accountId, 101, listOf("ana"))
        users.setAssignees(accountId, 101, listOf("luis"))

        users.observeCardAssignees(accountId, 1).test {
            assertEquals(
                listOf(
                    CardAssigneeRow(100, "ana", "Ana"),
                    CardAssigneeRow(100, "luis", "Luis"),
                    CardAssigneeRow(101, "luis", "Luis")
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `renaming a user keeps the assignment`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        users.upsert(listOf(DeckUserEntity(accountId, "ana", "Ana")))
        users.setAssignees(accountId, 100, listOf("ana"))

        users.upsert(listOf(DeckUserEntity(accountId, "ana", "Ana García")))

        users.observeCardAssignees(accountId, 1).test {
            assertEquals(listOf("Ana García"), awaitItem().map { it.displayName })
        }
    }

    @Test
    fun `tracks attachment upload states and lists pending uploads oldest first`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        attachments.upsert(
            listOf(
                attachment(accountId, 7),
                attachment(accountId, -1, UploadState.PENDING),
                attachment(accountId, -2, UploadState.PENDING)
            )
        )

        assertEquals(listOf(-1L, -2L), attachments.pendingUploads(accountId).map { it.id })
        attachments.setUploadState(accountId, -1, UploadState.UPLOADING)
        assertEquals(listOf(-2L), attachments.pendingUploads(accountId).map { it.id })
        attachments.observeForCard(accountId, 100).test {
            assertEquals(3, awaitItem().size)
        }
    }

    @Test
    fun `deleting the account removes labels, users and attachments`() = runTest {
        val accountId = Fixtures.boardWithCards(db, 100)
        labels.upsert(listOf(label(accountId, 1, "Bug")))
        users.upsert(listOf(DeckUserEntity(accountId, "ana", "Ana")))
        attachments.upsert(listOf(attachment(accountId, 7)))

        db.accountDao().delete(accountId)

        labels.observeForBoard(accountId, 1).test { assertTrue(awaitItem().isEmpty()) }
        attachments.observeForCard(accountId, 100).test { assertTrue(awaitItem().isEmpty()) }
    }

    @Test
    fun `keeps the data of each account apart`() = runTest {
        val ana = Fixtures.boardWithCards(db, 100)
        val luis = Fixtures.account(db, "luis")
        db.boardDao().upsert(listOf(Fixtures.board(luis)))
        db.stackDao().upsert(listOf(Fixtures.stack(luis)))
        db.cardDao().upsert(listOf(Fixtures.card(luis, 100)))
        labels.upsert(listOf(label(ana, 1, "Ana's"), label(luis, 1, "Luis's")))
        labels.setCardLabels(ana, 100, listOf(1))

        labels.observeCardLabels(luis, 1).test { assertTrue(awaitItem().isEmpty()) }
        labels.observeCardLabels(ana, 1).test {
            assertEquals(listOf("Ana's"), awaitItem().map { it.title })
        }
    }
}
