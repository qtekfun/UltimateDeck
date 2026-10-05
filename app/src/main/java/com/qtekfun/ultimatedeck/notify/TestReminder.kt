// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.Context
import androidx.core.content.edit
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import com.qtekfun.ultimatedeck.domain.reminders.TEST_CARD_ID
import com.qtekfun.ultimatedeck.domain.reminders.TestDelivery
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private const val PREFERENCES = "reminders"
private const val KEY_SCHEDULED = "test_scheduled"
private const val KEY_ARRIVED = "test_arrived"
private const val NO_TIME = -1L
private val DELAY: Duration = Duration.ofMinutes(1)

/**
 * A real test of the reminders (RF-10): an alarm a minute from now that travels the same way as
 * the real ones, and what became of it, so the user can tell whether the phone delivers them.
 */
@Singleton
class TestReminder @Inject constructor(
    @ApplicationContext context: Context,
    private val scheduler: ReminderScheduler,
    private val settings: SettingsRepository,
    private val clock: Clock
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** Sets the test alarm a minute from now, forgetting any earlier test. */
    suspend fun schedule(title: String) {
        val at = clock.instant().plus(DELAY)
        preferences.edit {
            putLong(KEY_SCHEDULED, at.toEpochMilli())
            remove(KEY_ARRIVED)
        }
        scheduler.scheduleTest(
            Reminder(NO_ACCOUNT, TEST_CARD_ID, 0, "", title, at, at),
            settings.settings.first().reminderAlarmClock
        )
    }

    /** The test alarm rang. */
    fun arrived() = preferences.edit { putLong(KEY_ARRIVED, clock.instant().toEpochMilli()) }

    /** How the last test went, or null when none was made. */
    fun delivery(): TestDelivery? = TestDelivery.of(
        time(KEY_SCHEDULED),
        time(KEY_ARRIVED),
        clock.instant()
    )

    private fun time(key: String): Instant? =
        preferences.getLong(key, NO_TIME).takeIf { it != NO_TIME }?.let(Instant::ofEpochMilli)
}
