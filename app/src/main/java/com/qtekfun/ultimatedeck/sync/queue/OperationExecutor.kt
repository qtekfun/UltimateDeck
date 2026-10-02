// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.queue

/** Sends one queued operation to the server; implemented by the sync engine (T09). */
fun interface OperationExecutor {
    suspend fun execute(entityId: Long, operation: QueuedOperation): ExecutionResult
}

/** What happened when an operation ran. */
sealed interface ExecutionResult {
    /** Applied. [serverId] is the id the server gave to an entity created offline. */
    data class Done(val serverId: Long? = null) : ExecutionResult

    /** Temporary problem (network, 5xx): try again later with backoff. */
    data class Retry(val reason: String?) : ExecutionResult

    /** The server refused it for good (e.g. file too large): wait for the user. */
    data class Failed(val reason: String?) : ExecutionResult
}
