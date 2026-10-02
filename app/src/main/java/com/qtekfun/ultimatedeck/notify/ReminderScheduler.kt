// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES = "reminders"
private const val KEY_SCHEDULED = "scheduled"

/**
 * Turns planned reminders into alarms (RF-10): exact when the system allows it, otherwise as
 * close as it lets. Alarms of a previous plan that are no longer wanted are cancelled.
 */
@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** False on Android 12+ until the user allows exact alarms for the app. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun schedule(reminders: List<Reminder>) {
        val wanted = reminders.associateBy { it.cardId }
        val previous = preferences.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().mapNotNull {
            it.toLongOrNull()
        }
        (previous - wanted.keys).forEach { alarms.cancel(pendingIntent(it, null)) }
        val exact = canScheduleExact()
        wanted.values.forEach { reminder ->
            val intent = pendingIntent(reminder.cardId, reminder)
            val at = reminder.at.toEpochMilli()
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        }
        preferences.edit { putStringSet(KEY_SCHEDULED, wanted.keys.map(Long::toString).toSet()) }
    }

    private fun pendingIntent(cardId: Long, reminder: Reminder?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction("reminder:$cardId")
        reminder?.let { ReminderReceiver.describe(intent, it) }
        return PendingIntent.getBroadcast(
            context,
            cardId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
