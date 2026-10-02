// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.mapper

import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.BoardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardEntity
import com.qtekfun.ultimatedeck.data.local.entity.CardServerSnapshotEntity
import com.qtekfun.ultimatedeck.data.local.entity.DeckUserEntity
import com.qtekfun.ultimatedeck.data.local.entity.LabelEntity
import com.qtekfun.ultimatedeck.data.local.entity.StackEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import com.qtekfun.ultimatedeck.data.remote.dto.AttachmentDto
import com.qtekfun.ultimatedeck.data.remote.dto.BoardDto
import com.qtekfun.ultimatedeck.data.remote.dto.CardDto
import com.qtekfun.ultimatedeck.data.remote.dto.LabelDto
import com.qtekfun.ultimatedeck.data.remote.dto.StackDto
import com.qtekfun.ultimatedeck.data.remote.dto.UpdateCardRequest
import com.qtekfun.ultimatedeck.data.remote.dto.UserDto

fun BoardDto.toEntity(accountId: Long) = BoardEntity(
    accountId = accountId,
    id = id,
    title = title,
    color = color,
    archived = archived,
    ownerUid = owner?.uid,
    lastModified = DeckDates.fromEpochSeconds(lastModified),
    etag = etag,
    deletedAt = DeckDates.fromEpochSeconds(deletedAt)
)

fun LabelDto.toEntity(accountId: Long, boardId: Long) =
    LabelEntity(accountId = accountId, id = id, boardId = boardId, title = title, color = color)

fun UserDto.toEntity(accountId: Long) =
    DeckUserEntity(accountId = accountId, uid = uid, displayName = displayName ?: uid)

fun StackDto.toEntity(accountId: Long) = StackEntity(
    accountId = accountId,
    id = id,
    boardId = boardId,
    title = title,
    order = order,
    lastModified = DeckDates.fromEpochSeconds(lastModified),
    etag = etag,
    deletedAt = DeckDates.fromEpochSeconds(deletedAt)
)

/** The card as stored locally, with nothing marked dirty. */
fun CardDto.toEntity(accountId: Long, boardId: Long) = CardEntity(
    accountId = accountId,
    id = id,
    boardId = boardId,
    stackId = stackId,
    title = title,
    description = description.orEmpty(),
    order = order,
    archived = archived,
    dueDate = DeckDates.fromIso(duedate),
    done = DeckDates.fromIso(done),
    ownerUid = owner?.uid,
    lastModified = DeckDates.fromEpochSeconds(lastModified),
    etag = etag,
    deletedAt = DeckDates.fromEpochSeconds(deletedAt),
    attachmentCount = attachmentCount ?: 0
)

/** The same card as the last state known from the server (SPEC §5). */
fun CardDto.toSnapshot(accountId: Long) = CardServerSnapshotEntity(
    accountId = accountId,
    cardId = id,
    title = title,
    description = description.orEmpty(),
    stackId = stackId,
    order = order,
    archived = archived,
    dueDate = DeckDates.fromIso(duedate),
    done = DeckDates.fromIso(done),
    labelIds = labels.orEmpty().map { it.id },
    assigneeUids = assignedUsers.orEmpty().map { it.participant.uid },
    lastModified = DeckDates.fromEpochSeconds(lastModified),
    etag = etag
)

fun AttachmentDto.toEntity(accountId: Long) = AttachmentEntity(
    accountId = accountId,
    id = id,
    cardId = cardId,
    fileName = data,
    mimeType = extendedData?.mimetype,
    size = extendedData?.filesize ?: 0,
    createdAt = DeckDates.fromEpochSeconds(createdAt),
    uploadState = UploadState.DONE
)

/** The whole editable state Deck expects in PUT …/cards/{id}. */
fun CardEntity.toUpdateRequest(fallbackOwner: String) = UpdateCardRequest(
    title = title,
    owner = ownerUid ?: fallbackOwner,
    order = order,
    description = description,
    duedate = DeckDates.toIso(dueDate),
    archived = archived,
    done = DeckDates.toIso(done)
)
