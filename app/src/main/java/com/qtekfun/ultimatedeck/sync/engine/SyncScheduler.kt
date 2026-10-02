// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PERIODIC = "sync-periodic"
private const val NOW = "sync-now"
private const val PERIOD_MINUTES = 15L
private const val BACKOFF_SECONDS = 30L

/** When syncs run (RF-08): every ~15 minutes with network, and on demand. */
@Singleton
class SyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Starts the periodic sync and syncs now, e.g. when the app opens with a session. */
    fun start() {
        workManager.enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
        )
        requestSync()
    }

    /** Syncs as soon as there is network; a request while one is pending is merged into it. */
    fun requestSync() {
        workManager.enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
        )
    }

    /** Stops every sync, at logout. */
    fun stop() {
        workManager.cancelUniqueWork(PERIODIC)
        workManager.cancelUniqueWork(NOW)
    }

    /** True while an on-demand sync is running, for the pull-to-refresh indicator. */
    fun syncing(): Flow<Boolean> = workManager.getWorkInfosForUniqueWorkFlow(NOW)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
}
