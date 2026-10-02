// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringExisting(attachments: List<AttachmentEntity>): List<Long>

    @Update
    suspend fun update(attachments: List<AttachmentEntity>)

    @Transaction
    suspend fun upsert(attachments: List<AttachmentEntity>) {
        val rowIds = insertIgnoringExisting(attachments)
        update(attachments.filterIndexed { index, _ -> rowIds[index] == -1L })
    }

    @Query(
        "SELECT * FROM attachment WHERE accountId = :accountId AND cardId = :cardId ORDER BY createdAt, id"
    )
    fun observeForCard(accountId: Long, cardId: Long): Flow<List<AttachmentEntity>>

    @Query("UPDATE attachment SET uploadState = :state WHERE accountId = :accountId AND id = :id")
    suspend fun setUploadState(accountId: Long, id: Long, state: UploadState)

    /** Attachments still to upload, oldest first (local ids count down from -1), for T17. */
    @Query(
        "SELECT * FROM attachment WHERE accountId = :accountId AND uploadState = 'PENDING' ORDER BY id DESC"
    )
    suspend fun pendingUploads(accountId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachment WHERE accountId = :accountId AND id = :id")
    suspend fun get(accountId: Long, id: Long): AttachmentEntity?

    @Query("DELETE FROM attachment WHERE accountId = :accountId AND id = :id")
    suspend fun delete(accountId: Long, id: Long)

    /** Attachments of a card that came from the server, to replace them with a fresh list. */
    @Query(
        "DELETE FROM attachment WHERE accountId = :accountId AND cardId = :cardId " +
            "AND uploadState = 'DONE'"
    )
    suspend fun deleteSynced(accountId: Long, cardId: Long)
}
