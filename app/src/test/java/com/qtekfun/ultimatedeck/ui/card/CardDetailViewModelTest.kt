// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.domain.card.CardActions
import com.qtekfun.ultimatedeck.domain.card.CardMetadataActions
import com.qtekfun.ultimatedeck.sync.engine.ACCOUNT
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.engine.card
import com.qtekfun.ultimatedeck.sync.engine.seedBoard
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val db = inMemoryDatabase()
    private val session = mockk<AccountSession>()
    private val actions = mockk<CardActions>(relaxed = true)
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private val metadata = mockk<CardMetadataActions>(relaxed = true)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `saves the latest text once typing pauses, and at once when leaving`() =
        runTest(dispatcher) {
            db.seedBoard()
            db.cardDao().upsert(listOf(card(5)))
            every { session.activeAccount } returns flowOf(db.accountDao().get(ACCOUNT))
            val viewModel = CardDetailViewModel(session, db, actions, metadata, scheduler)
            viewModel.open(5)
            runCurrent()

            viewModel.onTitleChange("T")
            viewModel.onTitleChange("Title")
            advanceTimeBy(SAVE_DELAY_MS - 1)
            coVerify(exactly = 0) { actions.editTitle(any(), any()) }
            advanceTimeBy(2)
            coVerify(exactly = 1) { actions.editTitle(5, "Title") }

            viewModel.saveNow("Final", "Notes")
            viewModel.resolve(CardField.TITLE, keepMine = false)
            runCurrent()
            coVerify { actions.editTitle(5, "Final") }
            coVerify { actions.editDescription(5, "Notes") }
            verify(exactly = 1) { scheduler.requestSync() }
            coVerify { actions.resolveConflict(5, CardField.TITLE, false) }
        }
}
