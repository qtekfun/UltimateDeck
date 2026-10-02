// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** The [DeckApi] of the signed-in account, or null when nobody is signed in. */
class AccountApiProvider @Inject constructor(
    private val factory: DeckApiFactory,
    private val session: AccountSession
) {
    suspend fun api(): DeckApi? = api(allowInsecure = false)

    /** [allowInsecure] exists only so tests can use a local plain-http server. */
    internal suspend fun api(allowInsecure: Boolean): DeckApi? {
        val account = session.activeAccount.first() ?: return null
        val server = ServerUrl.parse(
            account.serverUrl,
            allowInsecure
        ) as? ServerUrl.ParseResult.Valid
        return server?.let { factory.create(it.url, session) }
    }
}
