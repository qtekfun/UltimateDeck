// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

/** Why the last sync did not finish, as shown to the user. */
enum class SyncProblem { OFFLINE, UNAUTHORIZED, SERVER }

/** The problem to show after [this] outcome; null when the sync went fine or never ran. */
fun SyncOutcome?.toProblem(): SyncProblem? = when (this) {
    SyncOutcome.Offline -> SyncProblem.OFFLINE
    SyncOutcome.Unauthorized -> SyncProblem.UNAUTHORIZED
    is SyncOutcome.Error -> SyncProblem.SERVER
    is SyncOutcome.Ok, SyncOutcome.NoAccount, null -> null
}
