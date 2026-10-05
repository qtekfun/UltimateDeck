// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.qtekfun.ultimatedeck.data.local.entity.ShownReminderEntity
import java.time.Instant

/** The reminders already shown (RF-10). */
@Dao
interface ShownReminderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shown: ShownReminderEntity)

    @Query("SELECT * FROM shown_reminder WHERE accountId = :accountId")
    suspend fun all(accountId: Long): List<ShownReminderEntity>

    /** Forgets what was shown before [before]: no reminder that old is recovered any more. */
    @Query("DELETE FROM shown_reminder WHERE at < :before")
    suspend fun deleteBefore(before: Instant)
}
