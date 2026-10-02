// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CreateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.ReorderCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UpdateCardRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

private const val CARD = "boards/{boardId}/stacks/{stackId}/cards/{cardId}"

/** Cards. Paths are relative to …/apps/deck/api/v1.1/. */
interface CardApi {
    @GET(CARD)
    suspend fun getCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Header("If-None-Match") etag: String? = null
    ): Response<CardDto>

    @POST("boards/{boardId}/stacks/{stackId}/cards")
    suspend fun createCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Body card: CreateCardRequest
    ): Response<CardDto>

    @PUT(CARD)
    suspend fun updateCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body card: UpdateCardRequest
    ): Response<CardDto>

    /** Moves a card to [ReorderCardRequest.stackId] at [ReorderCardRequest.order]. */
    @PUT("$CARD/reorder")
    suspend fun reorderCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body position: ReorderCardRequest
    ): Response<List<CardDto>>

    @PUT("$CARD/archive")
    suspend fun archiveCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long
    ): Response<CardDto>

    @PUT("$CARD/unarchive")
    suspend fun unarchiveCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long
    ): Response<CardDto>

    @DELETE(CARD)
    suspend fun deleteCard(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long
    ): Response<Unit>
}
