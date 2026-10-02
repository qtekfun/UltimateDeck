// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.board

import app.cash.turbine.test
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.domain.board.BoardColumn
import com.qtekfun.ultimatedeck.domain.board.CardItem
import com.qtekfun.ultimatedeck.domain.board.CardLabel
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.BOARD
import com.qtekfun.ultimatedeck.sync.engine.STACK
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoardContentRepositoryTest {
    private val db = inMemoryDatabase()
    private val account = MutableStateFlow<AccountEntity?>(null)
    private val session = mockk<AccountSession> { every { activeAccount } returns account }
    private val repository = BoardContentRepository(session, db)
    private val due = Instant.parse("2026-10-04T10:00:00Z")

    @AfterEach
    fun close() = db.close()

    private fun card(id: Long, order: Int, stackId: Long = STACK) = CardEntity(
        accountId = ACCOUNT,
        id = id,
        boardId = BOARD,
        stackId = stackId,
        title = "Card $id",
        order = order
    )

    @Test
    fun `builds the columns with their cards and follows local edits`() = runTest {
        db.seedBoard()
        db.stackDao().upsert(listOf(StackEntity(ACCOUNT, 11, BOARD, "Done", 1)))
        db.labelDao().upsert(listOf(LabelEntity(ACCOUNT, 7, BOARD, "Bug", "ff0000")))
        db.userDao().upsert(listOf(DeckUserEntity(ACCOUNT, "bob", "Bob")))
        db.cardDao().upsert(
            listOf(
                card(
                    5,
                    order = 2
                ).copy(description = "- [x] a\n- [ ] b", dueDate = due, attachmentCount = 2),
                card(6, order = 1),
                card(7, order = 0, stackId = 11).copy(archived = true)
            )
        )
        db.labelDao().setCardLabels(ACCOUNT, 5, listOf(7))
        db.userDao().setAssignees(ACCOUNT, 5, listOf("bob"))

        repository.observeBoard(BOARD).test {
            assertEquals(emptyList<BoardColumn>(), awaitItem())
            account.value = db.accountDao().get(ACCOUNT)
            val columns = awaitItem()
            assertEquals(listOf("To do", "Done"), columns.map { it.title })
            assertEquals(listOf(6L, 5L), columns[0].cards.map { it.id })
            assertEquals(emptyList<CardItem>(), columns[1].cards)
            assertEquals(
                CardItem(
                    id = 5,
                    title = "Card 5",
                    description = "- [x] a\n- [ ] b",
                    labels = listOf(CardLabel("Bug", "ff0000")),
                    assignees = listOf("Bob"),
                    dueDate = due,
                    attachments = 2,
                    checklistDone = 1,
                    checklistTotal = 2,
                    pendingSync = false
                ),
                columns[0].cards[1]
            )

            db.cardLocalEditDao().markDirty(ACCOUNT, 6, CardField.TITLE.bit, due)
            // Room may emit once per changed table; the mark must arrive (or the test times out).
            while (!awaitItem()[0].cards[0].pendingSync) continue
            cancelAndIgnoreRemainingEvents()
        }
    }
}
