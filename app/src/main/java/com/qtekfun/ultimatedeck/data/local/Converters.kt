// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.local

import androidx.room3.ColumnTypeConverter
import java.time.Instant

/** Room type converters: instants as epoch milliseconds, lists as separator-joined text. */
class Converters {
    @ColumnTypeConverter
    fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @ColumnTypeConverter
    fun millisToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    @ColumnTypeConverter
    fun longsToText(values: List<Long>): String = values.joinToString(SEPARATOR)

    @ColumnTypeConverter
    fun textToLongs(text: String): List<Long> =
        if (text.isEmpty()) emptyList() else text.split(SEPARATOR).map(String::toLong)

    @ColumnTypeConverter
    fun stringsToText(values: List<String>): String = values.joinToString(SEPARATOR)

    @ColumnTypeConverter
    fun textToStrings(text: String): List<String> =
        if (text.isEmpty()) emptyList() else text.split(SEPARATOR)

    private companion object {
        /** ASCII unit separator: it never appears in Nextcloud user ids. */
        const val SEPARATOR = "\u001F"
    }
}
