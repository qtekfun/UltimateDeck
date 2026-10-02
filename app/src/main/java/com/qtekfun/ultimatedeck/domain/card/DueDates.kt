// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.domain.card

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Time given to a new due date picked without one: midday avoids slipping to another day. */
private val DEFAULT_TIME: LocalTime = LocalTime.NOON

/**
 * The due date for a day picked in a date picker, which reports UTC midnight of that day. The
 * time of the [previous] due date is kept; without one, midday in [zone].
 */
fun dueDateFor(pickedUtcMillis: Long, previous: Instant?, zone: ZoneId): Instant {
    val day: LocalDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val time = previous?.atZone(zone)?.toLocalTime() ?: DEFAULT_TIME
    return day.atTime(time).atZone(zone).toInstant()
}

/** The day of [dueDate] in [zone], as the UTC midnight millis a date picker expects. */
fun pickerMillisFor(dueDate: Instant, zone: ZoneId): Long =
    dueDate.atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
