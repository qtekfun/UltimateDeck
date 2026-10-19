// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncSchedulerPolicyTest {
    private fun policy(vararg pending: Pair<WorkInfo.State, Int>) =
        SyncScheduler.policyFor(pending.toList())

    @Test
    fun `with nothing pending a request is simply queued`() {
        assertEquals(ExistingWorkPolicy.KEEP, policy())
    }

    @Test
    fun `a queued or running sync absorbs the request`() {
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.ENQUEUED to 0))
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.RUNNING to 0))
        // A running retry is not cut short either.
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.RUNNING to 3))
    }

    @Test
    fun `a sync waiting out the delay after a failure is replaced, so it runs now`() {
        assertEquals(ExistingWorkPolicy.REPLACE, policy(WorkInfo.State.ENQUEUED to 1))
        assertEquals(ExistingWorkPolicy.REPLACE, policy(WorkInfo.State.ENQUEUED to 7))
    }

    @Test
    fun `finished work does not count`() {
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.SUCCEEDED to 2))
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.FAILED to 5))
        assertEquals(ExistingWorkPolicy.KEEP, policy(WorkInfo.State.CANCELLED to 1))
    }
}
