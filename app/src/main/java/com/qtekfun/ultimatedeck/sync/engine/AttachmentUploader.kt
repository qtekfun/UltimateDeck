// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.sync.engine

import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.DeckApi
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.sync.queue.ExecutionResult
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Uploads attachments added here (RF-07). The file was copied into the app when it was added,
 * so the upload survives restarts; its state is shown while it goes up.
 */
internal class AttachmentUploader(
    private val api: DeckApi,
    database: UltimateDeckDatabase,
    private val accountId: Long
) {
    private val attachments = database.attachmentDao()
    private val cards = database.cardDao()

    suspend fun upload(attachmentId: Long): ExecutionResult {
        val attachment = attachments.get(accountId, attachmentId) ?: return ExecutionResult.Done()
        // The card id is read now: a card created offline gets its server id before this runs.
        val card = cards.get(accountId, attachment.cardId)
        val file = attachment.localUri?.let(::File)?.takeIf { it.exists() }
        return when {
            card == null || card.id < 0 -> ExecutionResult.Retry("card not created yet")

            file == null -> {
                attachments.setUploadState(accountId, attachment.id, UploadState.FAILED)
                ExecutionResult.Failed("file missing")
            }

            else -> send(attachment, card, file)
        }
    }

    private suspend fun send(
        attachment: AttachmentEntity,
        card: CardEntity,
        file: File
    ): ExecutionResult {
        attachments.setUploadState(accountId, attachment.id, UploadState.UPLOADING)
        val part = MultipartBody.Part.createFormData(
            "file",
            attachment.fileName,
            file.asRequestBody(attachment.mimeType?.toMediaTypeOrNull())
        )
        val result = apiCall {
            api.attachments.upload(
                card.boardId,
                card.stackId,
                card.id,
                attachment.type.toRequestBody(),
                part
            )
        }
        if (result is ApiResult.Success) {
            // The server row replaces the local one; the local copy stays as its cached file.
            attachments.delete(accountId, attachment.id)
            attachments.upsert(
                listOf(result.value.toEntity(accountId).copy(localUri = attachment.localUri))
            )
            return ExecutionResult.Done(serverId = result.value.id)
        }
        val outcome = result.toExecutionResult()
        val state = when (outcome) {
            is ExecutionResult.Failed -> UploadState.FAILED
            else -> UploadState.PENDING
        }
        attachments.setUploadState(accountId, attachment.id, state)
        return outcome
    }
}
