// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.auth.basicAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the headers every Deck API request needs: basic auth with the app password, the OCS
 * header and JSON as the accepted type. Nothing here is logged.
 */
class DeckAuthInterceptor(private val credentials: CredentialsProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("OCS-APIRequest", "true")
            .header("Accept", "application/json")
        credentials.credentials()?.let {
            request.header(
                "Authorization",
                okhttp3.Credentials.basic(it.loginName, it.appPassword, Charsets.UTF_8)
            )
        }
        return chain.proceed(request.build())
    }
}
