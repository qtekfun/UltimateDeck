// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.reminders

import com.qtekfun.ultimatedeck.data.settings.AppSettings
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeepAlivePolicyTest {
    private val on = AppSettings(reminders = true, robustMode = true)

    @Test
    fun `runs with robust mode, reminders and a session`() {
        assertTrue(KeepAlivePolicy.shouldRun(on, signedIn = true))
    }

    @Test
    fun `does not run when any of them is missing`() {
        assertFalse(KeepAlivePolicy.shouldRun(on, signedIn = false))
        assertFalse(KeepAlivePolicy.shouldRun(on.copy(robustMode = false), signedIn = true))
        assertFalse(KeepAlivePolicy.shouldRun(on.copy(reminders = false), signedIn = true))
    }
}
