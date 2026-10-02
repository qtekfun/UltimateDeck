// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import javax.inject.Inject
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the [DeckApi] of an account. All accounts share the connection pool of [client];
 * each gets its own credentials. TLS validation is the system one, so user-installed CAs work
 * and nothing is ever trusted blindly (SPEC §6).
 */
class DeckApiFactory @Inject constructor(private val client: OkHttpClient, private val json: Json) {
    fun create(server: ServerUrl, credentials: CredentialsProvider): DeckApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.deckApi)
            .client(client.newBuilder().addInterceptor(DeckAuthInterceptor(credentials)).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return DeckApi(
            boards = retrofit.create(BoardApi::class.java),
            cards = retrofit.create(CardApi::class.java),
            cardMetadata = retrofit.create(CardMetadataApi::class.java),
            attachments = retrofit.create(AttachmentApi::class.java)
        )
    }
}
