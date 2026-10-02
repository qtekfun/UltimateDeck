// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

/** Runs a sync in the background; WorkManager retries it with backoff when it fails. */
class SyncWorker(context: Context, params: WorkerParameters, private val engine: SyncEngine) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (engine.sync()) {
        is SyncOutcome.Ok, SyncOutcome.NoAccount -> Result.success()

        SyncOutcome.Offline, is SyncOutcome.Error -> Result.retry()

        // Retrying cannot help until the user signs in again.
        SyncOutcome.Unauthorized -> Result.failure()
    }
}

/** Creates workers with their dependencies, without an extra Hilt-WorkManager library. */
class SyncWorkerFactory @Inject constructor(private val engine: Provider<SyncEngine>) :
    WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = if (workerClassName == SyncWorker::class.java.name) {
        SyncWorker(appContext, workerParameters, engine.get())
    } else {
        null
    }
}
