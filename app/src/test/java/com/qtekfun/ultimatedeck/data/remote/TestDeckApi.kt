// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.DeckJson
import java.util.concurrent.TimeUnit
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

const val API_PATH = "/index.php/apps/deck/api/v1.1/"

/** A [DeckApi] pointing at [server], with short timeouts so timeout tests are fast. */
fun testDeckApi(server: MockWebServer): DeckApi {
    val client = OkHttpClient.Builder()
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(server.url(API_PATH))
        .client(client)
        .addConverterFactory(DeckJson.asConverterFactory("application/json".toMediaType()))
        .build()
    return DeckApi(
        boards = retrofit.create(BoardApi::class.java),
        cards = retrofit.create(CardApi::class.java),
        cardMetadata = retrofit.create(CardMetadataApi::class.java),
        attachments = retrofit.create(AttachmentApi::class.java)
    )
}

fun json(body: String, code: Int = 200, vararg headers: String) =
    MockResponse(code, headersOf("Content-Type", "application/json", *headers), body)
