// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote

import com.qtekfun.ultimatedeck.data.remote.dto.AttachmentDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

private const val ATTACHMENTS = "boards/{boardId}/stacks/{stackId}/cards/{cardId}/attachments"

/** Card attachments, API v1.1: each one has a type, `deck_file` or `file`. */
interface AttachmentApi {
    @GET(ATTACHMENTS)
    suspend fun getAttachments(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Header("If-None-Match") etag: String? = null
    ): Response<List<AttachmentDto>>

    /** Uploads [file] as an attachment of [type]. */
    @Multipart
    @POST(ATTACHMENTS)
    suspend fun upload(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Part("type") type: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<AttachmentDto>

    /** Streams the attachment content, for on-demand downloads (RF-07). */
    @Streaming
    @GET("$ATTACHMENTS/{type}/{attachmentId}")
    suspend fun download(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Path("type") type: String,
        @Path("attachmentId") attachmentId: Long
    ): Response<ResponseBody>

    @DELETE("$ATTACHMENTS/{type}/{attachmentId}")
    suspend fun delete(
        @Path("boardId") boardId: Long,
        @Path("stackId") stackId: Long,
        @Path("cardId") cardId: Long,
        @Path("type") type: String,
        @Path("attachmentId") attachmentId: Long
    ): Response<Unit>
}
