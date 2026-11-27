// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import com.qtekfun.ultimatedeck.data.board.BoardContentRepository
import com.qtekfun.ultimatedeck.sync.engine.SyncEngine
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val syncing = MutableStateFlow(false)
    private val repository = mockk<BoardContentRepository> {
        every { observeBoard(1) } returns flowOf(emptyList())
    }
    private val engine = mockk<SyncEngine> {
        every { lastOutcome } returns MutableStateFlow(null)
    }
    private val scheduler = mockk<SyncScheduler>(relaxUnitFun = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { scheduler.syncing() } returns syncing
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.openBoard(): BoardViewModel {
        val viewModel = BoardViewModel(repository, mockk(), engine, scheduler)
        viewModel.open(1)
        runCurrent()
        return viewModel
    }

    @Test
    fun `an automatic sync does not show the indicator`() = runTest(dispatcher) {
        val viewModel = openBoard()

        syncing.value = true
        runCurrent()

        assertFalse(viewModel.state.value.refreshing)
    }

    @Test
    fun `pulling shows the indicator until the sync ends`() = runTest(dispatcher) {
        val viewModel = openBoard()

        viewModel.refresh()
        runCurrent()
        syncing.value = true
        advanceTimeBy(60_000)
        assertTrue(viewModel.state.value.refreshing)

        syncing.value = false
        runCurrent()

        assertFalse(viewModel.state.value.refreshing)
        verify(exactly = 1) { scheduler.requestSync() }
    }

    @Test
    fun `the indicator hides when the sync cannot start`() = runTest(dispatcher) {
        val viewModel = openBoard()

        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.state.value.refreshing)
        advanceTimeBy(5_001)

        assertFalse(viewModel.state.value.refreshing)
    }
}
