// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CreateBoardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.CreateStackRequest
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Boards and columns (stacks). Paths are relative to …/apps/deck/api/v1.1/. */
interface BoardApi {
    /** All boards of the user, with labels and users when [details] is true. */
    @GET("boards")
    suspend fun getBoards(
        @Query("details") details: Boolean = true,
        @Header("If-None-Match") etag: String? = null,
        @Header("If-Modified-Since") modifiedSince: String? = null
    ): Response<List<BoardDto>>

    @GET("boards/{boardId}")
    suspend fun getBoard(
        @Path("boardId") boardId: Long,
        @Header("If-None-Match") etag: String? = null
    ): Response<BoardDto>

    /** The columns of a board, each with its cards. */
    @GET("boards/{boardId}/stacks")
    suspend fun getStacks(
        @Path("boardId") boardId: Long,
        @Header("If-None-Match") etag: String? = null,
        @Header("If-Modified-Since") modifiedSince: String? = null
    ): Response<List<StackDto>>

    /** The columns of a board with only their archived cards. */
    @GET("boards/{boardId}/stacks/archived")
    suspend fun getArchivedStacks(@Path("boardId") boardId: Long): Response<List<StackDto>>

    @POST("boards")
    suspend fun createBoard(@Body request: CreateBoardRequest): Response<BoardDto>

    @POST("boards/{boardId}/stacks")
    suspend fun createStack(
        @Path("boardId") boardId: Long,
        @Body request: CreateStackRequest
    ): Response<StackDto>

    /** Deletes a board; Deck keeps it restorable from its web interface for a while. */
    @DELETE("boards/{boardId}")
    suspend fun deleteBoard(@Path("boardId") boardId: Long): Response<ResponseBody>

    @DELETE("boards/{boardId}/stacks/{stackId}")
    suspend fun deleteStack(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long
    ): Response<ResponseBody>
}
