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
import com.qtekfun.ultimatedeck.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES = "reminders"
private const val KEY_SCHEDULED = "scheduled"

/**
 * Turns planned reminders into alarms (RF-10): alarm clocks when exact alarms are allowed, so
 * they ring on time with the screen off; otherwise as close as the system lets. Alarms of a
 * previous plan that are no longer wanted are cancelled.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heartbeat: HeartbeatScheduler
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** False on Android 12+ until the user allows exact alarms for the app. */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    /** [alarmClock]: the aggressive mode, set like an alarm clock so nothing delays it. */
    fun schedule(reminders: List<Reminder>, alarmClock: Boolean) {
        val wanted = reminders.associateBy { it.cardId }
        val previous = preferences.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().mapNotNull {
            it.toLongOrNull()
        }
        (previous - wanted.keys).forEach { alarms.cancel(pendingIntent(it, null)) }
        wanted.values.forEach { set(it, alarmClock) }
        heartbeat.update(reminders)
        preferences.edit { putStringSet(KEY_SCHEDULED, wanted.keys.map(Long::toString).toSet()) }
    }

    /**
     * Sets the test reminder the same way as real ones. It is kept out of the scheduled set, so
     * planning real reminders never cancels it.
     */
    fun scheduleTest(reminder: Reminder, alarmClock: Boolean) = set(reminder, alarmClock)

    private fun set(reminder: Reminder, alarmClock: Boolean) {
        val intent = pendingIntent(reminder.cardId, reminder)
        val at = reminder.at.toEpochMilli()
        when {
            // An alarm clock is never deferred, not even by battery savers that delay other
            // exact alarms with the screen off (seen on ColorOS); it shows the alarm icon.
            canScheduleExact() && alarmClock ->
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, openCard(reminder)), intent)

            canScheduleExact() ->
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)

            else -> alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    /** What the system opens from its "next alarm" display: the card. */
    private fun openCard(reminder: Reminder): PendingIntent = PendingIntent.getActivity(
        context,
        reminder.cardId.hashCode(),
        CardLink(reminder.boardId, reminder.boardTitle, reminder.cardId)
            .putInto(Intent(context, MainActivity::class.java))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

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
