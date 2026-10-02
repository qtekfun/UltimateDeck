// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.di.IoDispatcher
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.ProcessResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** How a sync ended. */
sealed interface SyncOutcome {
    /** Pulled; [pushed] tells what happened to the queued changes. */
    data class Ok(val pushed: ProcessResult) : SyncOutcome

    data object NoAccount : SyncOutcome

    data object Offline : SyncOutcome

    data object Unauthorized : SyncOutcome

    data class Error(val reason: String) : SyncOutcome
}

/**
 * Syncs the signed-in account: first sends the queued local changes, then pulls the server
 * state, so the conflict resolver only sees real conflicts. Syncs never overlap: one started
 * while another runs waits for it.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val session: AccountSession,
    private val apiProvider: AccountApiProvider,
    private val database: UltimateDeckDatabase,
    private val queue: OperationQueue,
    private val pull: PullSync,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private val mutableLastOutcome = MutableStateFlow<SyncOutcome?>(null)

    /** How the latest sync of this process ended; null until one finishes. */
    val lastOutcome: StateFlow<SyncOutcome?> = mutableLastOutcome.asStateFlow()

    suspend fun sync(): SyncOutcome = withContext(dispatcher) {
        mutex.withLock { run().also { mutableLastOutcome.value = it } }
    }

    private suspend fun run(): SyncOutcome {
        // A background sync may start in a fresh process, before the UI loaded the credentials.
        if (session.credentials() == null) session.restore()
        val account = session.activeAccount.first()
        val api = account?.let { apiProvider.api() } ?: return SyncOutcome.NoAccount
        val executor = DeckOperationExecutor(api, database, account.id, account.userId)
        val pushed = queue.process(account.id, executor)
        return when (val pulled = pull.pull(api, account.id)) {
            PullResult.Done -> SyncOutcome.Ok(pushed)
            is PullResult.Failed -> pulled.cause.toOutcome()
        }
    }

    private fun ApiResult<*>.toOutcome(): SyncOutcome = when (this) {
        is ApiResult.NetworkError -> SyncOutcome.Offline
        ApiResult.Unauthorized -> SyncOutcome.Unauthorized
        is ApiResult.HttpError -> SyncOutcome.Error("HTTP $code")
        else -> SyncOutcome.Error(toString())
    }
}
