// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.remote.mapper

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Date formats of the Deck API. */
object DeckDates {
    private val requestFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")

    /** Epoch seconds, where Deck uses 0 (or nothing) for "not set". */
    fun fromEpochSeconds(seconds: Long?): Instant? =
        seconds?.takeIf { it > 0 }?.let(Instant::ofEpochSecond)

    /** ISO-8601 with offset, as in `duedate`; unparseable values count as not set. */
    fun fromIso(text: String?): Instant? = text?.takeIf { it.isNotBlank() }?.let {
        try {
            OffsetDateTime.parse(it).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** The format Deck accepts in requests, in UTC: `2026-10-04T10:00:00+00:00`. */
    fun toIso(instant: Instant?): String? = instant?.atOffset(ZoneOffset.UTC)?.format(requestFormat)
}
