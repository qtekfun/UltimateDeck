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
import java.util.concurrent.Executors
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

    /** Requests are decided one at a time, so two of them cannot both replace the pending sync. */
    private val requests = Executors.newSingleThreadExecutor()

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

    /**
     * Syncs as soon as there is network. A request while a sync is queued or running is merged
     * into it, except when the queued one is waiting out the retry delay of a failure: that delay
     * grows up to hours, and opening the app or pulling to refresh must not wait for it.
     */
    fun requestSync() {
        requests.execute {
            val pending = runCatching { workManager.getWorkInfosForUniqueWork(NOW).get() }
                .getOrDefault(emptyList())
            val policy = policyFor(pending.map { it.state to it.runAttemptCount })
            runCatching {
                workManager.enqueueUniqueWork(
                    NOW,
                    policy,
                    OneTimeWorkRequestBuilder<SyncWorker>()
                        .setConstraints(online)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            BACKOFF_SECONDS,
                            TimeUnit.SECONDS
                        )
                        .build()
                ).result.get()
            }
        }
    }

    /** Stops every sync, at logout. */
    fun stop() {
        workManager.cancelUniqueWork(PERIODIC)
        workManager.cancelUniqueWork(NOW)
    }

    companion object {
        /**
         * How a new request treats the pending one, given its state and attempts so far: it
         * replaces a sync that already failed and waits for its retry, and keeps any other.
         */
        fun policyFor(pending: List<Pair<WorkInfo.State, Int>>): ExistingWorkPolicy {
            val waitingToRetry = pending.any { (state, attempts) ->
                state == WorkInfo.State.ENQUEUED && attempts > 0
            }
            return if (waitingToRetry) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        }
    }

    /** True while an on-demand sync is running, for the pull-to-refresh indicator. */
    fun syncing(): Flow<Boolean> = workManager.getWorkInfosForUniqueWorkFlow(NOW)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
}
