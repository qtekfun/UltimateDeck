// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.auth

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.remote.CredentialsProvider
import com.qtekfun.ultimatedeck.data.remote.ServerUrl
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The signed-in account. The MVP shows a single account, the first one; the data model already
 * supports several. Decrypted credentials are kept only in memory and handed to the API client.
 */
@Singleton
class AccountSession @Inject constructor(
    database: UltimateDeckDatabase,
    private val credentialStore: CredentialStore
) : CredentialsProvider {
    private val accounts = database.accountDao()

    @Volatile
    private var current: Credentials? = null

    val activeAccount: Flow<AccountEntity?> = accounts.observeAll().map { it.firstOrNull() }

    /** Loads the stored credentials of the active account, e.g. when the app starts. */
    suspend fun restore(): AccountEntity? {
        val account = activeAccount.first()
        current = account?.let { credentialStore.load(it.id) }
        return account
    }

    /** Creates the account and stores its encrypted credentials; returns the account id. */
    suspend fun signIn(server: ServerUrl, credentials: Credentials): Long {
        val accountId = accounts.insert(
            AccountEntity(
                serverUrl = server.root.toString(),
                userId = credentials.loginName,
                displayName = credentials.loginName
            )
        )
        credentialStore.save(accountId, credentials)
        current = credentials
        return accountId
    }

    /** Forgets the in-memory credentials; the stored ones go with the account. */
    fun forget() {
        current = null
    }

    override fun credentials(): Credentials? = current
}
