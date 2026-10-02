// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.LabelIdRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserIdRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

private const val CARD = "boards/{boardId}/stacks/{stackId}/cards/{cardId}"

/** Labels and assignees of a card. Paths are relative to …/apps/deck/api/v1.1/. */
interface CardMetadataApi {
    @PUT("$CARD/assignLabel")
    suspend fun assignLabel(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body label: LabelIdRequest
    ): Response<Unit>

    @PUT("$CARD/removeLabel")
    suspend fun removeLabel(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body label: LabelIdRequest
    ): Response<Unit>

    @PUT("$CARD/assignUser")
    suspend fun assignUser(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body user: UserIdRequest
    ): Response<Unit>

    @PUT("$CARD/unassignUser")
    suspend fun unassignUser(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Body user: UserIdRequest
    ): Response<Unit>
}
