// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.qtekfun.ultimatedeck.sync.queue.ProcessResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SyncWorkerTest {
    private val engine = mockk<SyncEngine>()
    private val factory = SyncWorkerFactory { engine }
    private val context = mockk<Context>(relaxed = true)
    private val params = mockk<WorkerParameters>(relaxed = true)

    private suspend fun resultFor(outcome: SyncOutcome): ListenableWorker.Result {
        coEvery { engine.sync() } returns outcome
        val worker = factory.createWorker(
            context,
            SyncWorker::class.java.name,
            params
        ) as SyncWorker
        return worker.doWork()
    }

    @Test
    fun `a finished sync or no account succeeds`() = runTest {
        assertEquals(ListenableWorker.Result.success(), resultFor(SyncOutcome.Ok(ProcessResult())))
        assertEquals(ListenableWorker.Result.success(), resultFor(SyncOutcome.NoAccount))
    }

    @Test
    fun `temporary problems are retried with backoff`() = runTest {
        assertEquals(ListenableWorker.Result.retry(), resultFor(SyncOutcome.Offline))
        assertEquals(ListenableWorker.Result.retry(), resultFor(SyncOutcome.Error("HTTP 500")))
    }

    @Test
    fun `a rejected session stops retrying`() = runTest {
        assertEquals(ListenableWorker.Result.failure(), resultFor(SyncOutcome.Unauthorized))
    }

    @Test
    fun `the factory only creates its own worker`() {
        assertNull(factory.createWorker(context, "other.Worker", params))
        assertTrue(factory.createWorker(context, SyncWorker::class.java.name, params) is SyncWorker)
    }
}
