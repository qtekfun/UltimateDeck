// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.sync.queue.ProcessResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncProblemTest {
    @Test
    fun `only unfinished syncs are problems`() {
        assertEquals(SyncProblem.OFFLINE, SyncOutcome.Offline.toProblem())
        assertEquals(SyncProblem.UNAUTHORIZED, SyncOutcome.Unauthorized.toProblem())
        assertEquals(SyncProblem.SERVER, SyncOutcome.Error("HTTP 500").toProblem())
        assertEquals(null, SyncOutcome.Ok(ProcessResult()).toProblem())
        assertEquals(null, SyncOutcome.NoAccount.toProblem())
        assertEquals(null, (null as SyncOutcome?).toProblem())
    }
}
