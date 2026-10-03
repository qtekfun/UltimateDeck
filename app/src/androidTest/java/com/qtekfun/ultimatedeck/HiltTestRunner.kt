// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import dagger.hilt.android.testing.CustomTestApplication

/** Base of the test app: WorkManager needs a configuration, the default initializer is off. */
open class TestAppBase :
    Application(),
    Configuration.Provider {
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().build()
}

/** The Hilt app used by UI tests, generated from [TestAppBase]. */
@CustomTestApplication(TestAppBase::class)
interface UiTestApplication

/** Runs UI tests on the Hilt test app, so modules can be replaced with test ones. */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, UiTestApplication_Application::class.java.name, context)
}
