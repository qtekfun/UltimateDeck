// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.auth

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.auth.LoginFlowApiFactory
import com.qtekfun.ultimatedeck.data.auth.basicAuth
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.ServerUrl
import com.qtekfun.ultimatedeck.data.remote.apiCall
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Logs out (RF-01): revokes the app password on the server, then deletes the account, which
 * removes its credentials and all its local data. Revocation is best effort: as Nextcloud
 * recommends, the account is removed even if the server cannot be reached.
 */
class Logout @Inject constructor(
    private val apiFactory: LoginFlowApiFactory,
    private val session: AccountSession,
    private val credentialStore: CredentialStore,
    database: UltimateDeckDatabase
) {
    private val accounts = database.accountDao()

    suspend operator fun invoke() = run(allowInsecure = false)

    /** [allowInsecure] exists only so tests can revoke against a local plain-http server. */
    internal suspend fun run(allowInsecure: Boolean) {
        val account = session.activeAccount.first() ?: return
        val credentials = session.credentials() ?: credentialStore.load(account.id)
        val server = ServerUrl.parse(
            account.serverUrl,
            allowInsecure
        ) as? ServerUrl.ParseResult.Valid
        if (credentials != null && server != null) {
            apiCall { apiFactory.create(server.url).revokeAppPassword(basicAuth(credentials)) }
        }
        accounts.delete(account.id)
        session.forget()
    }
}
