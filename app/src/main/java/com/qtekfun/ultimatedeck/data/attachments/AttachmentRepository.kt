// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.attachments

import android.net.Uri
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.local.model.EntityType
import com.qtekfun.ultimatedeck.data.remote.AccountApiProvider
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import com.qtekfun.ultimatedeck.data.remote.apiCall
import com.qtekfun.ultimatedeck.data.remote.map
import com.qtekfun.ultimatedeck.data.remote.mapper.toEntity
import com.qtekfun.ultimatedeck.sync.engine.SyncScheduler
import com.qtekfun.ultimatedeck.sync.queue.OperationQueue
import com.qtekfun.ultimatedeck.sync.queue.QueuedOperation
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Attachments of a card (RF-07): added offline and uploaded through the queue, listed from
 * Room, and downloaded only when opened.
 */
class AttachmentRepository @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val files: AttachmentFiles,
    private val apis: AccountApiProvider,
    private val queue: OperationQueue,
    private val scheduler: SyncScheduler
) {
    private val attachments = database.attachmentDao()
    private val cards = database.cardDao()
    private val localIds = database.localIdDao()
    private val operations = database.pendingOperationDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(cardId: Long): Flow<List<AttachmentEntity>> = session.activeAccount.flatMapLatest {
        if (it == null) flowOf(emptyList()) else attachments.observeForCard(it.id, cardId)
    }

    /** Copies the file into the app and queues its upload; false if it could not be read. */
    suspend fun add(cardId: Long, uri: Uri): Boolean {
        val card = accountId()?.let { cards.get(it, cardId) } ?: return false
        val id = localIds.nextId(card.accountId, EntityType.ATTACHMENT)
        return files.import(uri, card.accountId, id)?.let { queueUpload(card, id, it) } ?: false
    }

    private suspend fun queueUpload(card: CardEntity, id: Long, imported: ImportedFile): Boolean {
        attachments.upsert(
            listOf(
                AttachmentEntity(
                    accountId = card.accountId,
                    id = id,
                    cardId = card.id,
                    fileName = imported.name,
                    mimeType = imported.mimeType,
                    size = imported.file.length(),
                    localUri = imported.file.path,
                    uploadState = UploadState.PENDING
                )
            )
        )
        queue.enqueue(
            card.accountId,
            id,
            QueuedOperation.UploadAttachment(card.boardId, card.stackId, card.id)
        )
        scheduler.requestSync()
        return true
    }

    /** Replaces the synced attachments with the server's list; pending uploads stay. */
    suspend fun refresh(cardId: Long): ApiResult<Unit> {
        // Cards still created only here have nothing on the server yet.
        val card = accountId()?.let { cards.get(it, cardId) }?.takeIf { it.id > 0 }
        val api = apis.api()
        if (card == null || api == null) return ApiResult.Success(Unit)
        val accountId = card.accountId
        val result = apiCall { api.attachments.getAttachments(card.boardId, card.stackId, card.id) }
        if (result is ApiResult.Success) {
            val cached = attachments.observeForCard(accountId, cardId).first().associate {
                it.id to
                    it.localUri
            }
            attachments.deleteSynced(accountId, cardId)
            attachments.upsert(
                result.value.filter { it.deletedAt == 0L }
                    .map { it.toEntity(accountId).copy(localUri = cached[it.id]) }
            )
        }
        return result.map { }
    }

    /** The attachment's file, downloaded now if this device does not have it yet. */
    suspend fun open(attachment: AttachmentEntity): ApiResult<File> {
        val cached = attachment.localUri?.let(::File)?.takeIf { it.exists() }
        return if (cached != null) ApiResult.Success(cached) else download(attachment)
    }

    private suspend fun download(attachment: AttachmentEntity): ApiResult<File> {
        val card = cards.get(attachment.accountId, attachment.cardId)
        val api = apis.api()
        if (card == null || api == null) return ApiResult.Unauthorized
        val result = apiCall {
            api.attachments.download(
                card.boardId,
                card.stackId,
                card.id,
                attachment.type,
                attachment.id
            )
        }
        return result.map { body ->
            val target = files.fileFor(attachment.accountId, attachment.id, attachment.fileName)
            body.byteStream().use { files.save(it, target) }
            attachments.update(listOf(attachment.copy(localUri = target.path)))
            target
        }
    }

    /** Tries a failed upload again. */
    suspend fun retry(attachment: AttachmentEntity) {
        operations.forEntity(attachment.accountId, EntityType.ATTACHMENT, attachment.id)
            .forEach { queue.retry(it.id) }
        attachments.setUploadState(attachment.accountId, attachment.id, UploadState.PENDING)
        scheduler.requestSync()
    }

    /** Gives up a failed upload: nothing is sent and the copy is deleted. */
    suspend fun discard(attachment: AttachmentEntity) {
        operations.forEntity(attachment.accountId, EntityType.ATTACHMENT, attachment.id)
            .forEach { queue.discard(it.id) }
        attachments.delete(attachment.accountId, attachment.id)
        attachment.localUri?.let { files.delete(it) }
    }

    private suspend fun accountId() = session.activeAccount.first()?.id
}
