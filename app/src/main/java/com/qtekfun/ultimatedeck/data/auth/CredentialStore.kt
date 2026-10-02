// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.auth

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AccountCredentialsEntity
import com.qtekfun.ultimatedeck.data.remote.Credentials
import javax.inject.Inject

/** Stores app passwords encrypted at rest; plaintext only ever exists in memory. */
class CredentialStore @Inject constructor(
    database: UltimateDeckDatabase,
    private val cipher: SecretCipher
) {
    private val dao = database.credentialsDao()

    suspend fun save(accountId: Long, credentials: Credentials) {
        val secret = cipher.encrypt(credentials.appPassword.toByteArray(Charsets.UTF_8))
        dao.put(
            AccountCredentialsEntity(accountId, credentials.loginName, secret.ciphertext, secret.iv)
        )
    }

    suspend fun load(accountId: Long): Credentials? = dao.get(accountId)?.let { stored ->
        val password = cipher.decrypt(EncryptedSecret(stored.ciphertext, stored.iv))
        Credentials(stored.loginName, password.toString(Charsets.UTF_8))
    }
}
