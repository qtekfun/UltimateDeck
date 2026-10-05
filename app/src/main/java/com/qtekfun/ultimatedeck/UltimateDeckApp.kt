// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck

import android.app.Application
import androidx.work.Configuration
import com.qtekfun.ultimatedeck.notify.KeepAliveController
import com.qtekfun.ultimatedeck.notify.ReminderCoordinator
import com.qtekfun.ultimatedeck.sync.engine.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class UltimateDeckApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: SyncWorkerFactory

    @Inject
    lateinit var reminders: ReminderCoordinator

    @Inject
    lateinit var keepAlive: KeepAliveController

    /** Lives as long as the process: keeps the reminders scheduled (RF-10). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        reminders.start(appScope)
        keepAlive.start(appScope)
    }

    /** WorkManager starts on demand with this factory; its default initializer is off. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
