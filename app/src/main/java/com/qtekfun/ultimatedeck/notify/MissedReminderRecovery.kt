// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.Context
import androidx.core.content.edit
import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.ShownReminderEntity
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.domain.reminders.MissedReminders
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import com.qtekfun.ultimatedeck.domain.reminders.ReminderPlanner
import com.qtekfun.ultimatedeck.domain.reminders.ShownReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What an alarm carries when it was set by an earlier version. */
internal const val NO_ACCOUNT = -1L

private const val PREFERENCES = "reminders"
private const val KEY_SINCE = "recovery_since"
private const val WINDOW_HOURS = 24L
private val WINDOW: Duration = Duration.ofHours(WINDOW_HOURS)

/** An alarm this close to its time may still be on its way: not missed yet. */
private const val GRACE_MINUTES = 2L
private val GRACE: Duration = Duration.ofMinutes(GRACE_MINUTES)

/**
 * Brings back the reminders whose alarm never rang, because the system stopped the app (RF-10):
 * it keeps what was shown and, at start, after a sync and when any alarm comes in, shows the
 * rest of the last 24 hours as missed. What was already past when it began is left alone.
 */
@Singleton
class MissedReminderRecovery @Inject constructor(
    @ApplicationContext context: Context,
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val settings: SettingsRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock
) {
    private val dueCards = database.reminderDao()
    private val shownReminders = database.shownReminderDao()
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val lock = Mutex()

    /** Notes that [reminder] was shown, for the signed-in account only. */
    suspend fun markShown(reminder: Reminder) {
        if (session.activeAccount.first()?.id == reminder.accountId) {
            shownReminders.insert(
                ShownReminderEntity(reminder.accountId, reminder.cardId, reminder.at)
            )
        }
    }

    /** Shows the reminders that were missed, once each. */
    suspend fun recover() = lock.withLock {
        val current = settings.settings.first()
        val account = session.activeAccount.first()
        if (account == null || !current.reminders || !current.recoverMissed) {
            preferences.edit { remove(KEY_SINCE) }
            return@withLock
        }
        val now = clock.instant()
        val since = since(now)
        val planned = ReminderPlanner.planAll(
            dueCards.dueCards(account.id, account.userId),
            current
        ).filter { !it.at.isAfter(now.minus(GRACE)) }
        val shown = shownReminders.all(account.id).map { ShownReminder(it.cardId, it.at) }
        MissedReminders.pick(planned, shown.toSet(), now, WINDOW, since).forEach {
            if (notifier.show(it, missed = true)) markShown(it)
        }
        shownReminders.deleteBefore(MissedReminders.keepAfter(now, WINDOW))
    }

    /** When recovery began: the first time reminders were on and recovered since. */
    private fun since(now: Instant): Instant {
        val stored = preferences.getLong(KEY_SINCE, -1)
        if (stored >= 0) return Instant.ofEpochMilli(stored)
        preferences.edit { putLong(KEY_SINCE, now.toEpochMilli()) }
        return now
    }
}
