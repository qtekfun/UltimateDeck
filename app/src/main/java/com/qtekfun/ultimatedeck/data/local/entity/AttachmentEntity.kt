// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import java.time.Instant

/** Where an attachment is in its upload (RF-07); downloaded ones are [UploadState.DONE]. */
enum class UploadState { PENDING, UPLOADING, FAILED, DONE }

/**
 * A card attachment. [localUri] points to the local copy: the file to upload, or the cached
 * download once it was opened (downloads are on demand).
 */
@Entity(
    tableName = "attachment",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "cardId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId", "cardId")]
)
data class AttachmentEntity(
    val accountId: Long,
    val id: Long,
    val cardId: Long,
    val fileName: String,
    val mimeType: String?,
    val size: Long,
    val createdAt: Instant? = null,
    val localUri: String? = null,
    val uploadState: UploadState = UploadState.DONE,
    /** Deck's attachment type, part of its download path: "file", or "deck_file" for old ones. */
    val type: String = "file"
)
