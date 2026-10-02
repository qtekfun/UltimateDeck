// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

/** Login name and app password of an account (RF-01). The password never shows in [toString]. */
class Credentials(val loginName: String, val appPassword: String) {
    override fun toString(): String = "Credentials(loginName=$loginName, appPassword=***)"
}

/** Where the API client gets the current credentials; T06 reads them from the Keystore. */
fun interface CredentialsProvider {
    fun credentials(): Credentials?
}
