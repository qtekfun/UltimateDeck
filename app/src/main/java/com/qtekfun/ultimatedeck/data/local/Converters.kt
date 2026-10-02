// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.ColumnTypeConverter
import java.time.Instant

/** Room type converters. Instants are stored as epoch milliseconds. */
class Converters {
    @ColumnTypeConverter
    fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @ColumnTypeConverter
    fun millisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)
}
