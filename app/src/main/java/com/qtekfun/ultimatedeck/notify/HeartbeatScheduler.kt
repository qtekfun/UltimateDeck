// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qtekfun.ultimatedeck.domain.reminders.Heartbeat
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets the heartbeat alarm (RF-10): one silent alarm every 30 minutes while some reminder is
 * pending. It is not an alarm clock, so it shows no icon; Doze lets an exact alarm through
 * every few minutes, plenty for this. Cancelled when no reminder is left.
 */
@Singleton
class HeartbeatScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun update(pending: List<Reminder>) {
        val next = Heartbeat.next(clock.instant(), pending)
        if (next == null) {
            alarms.cancel(pendingIntent())
            return
        }
        val at = next.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent())
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent())
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, HeartbeatReceiver::class.java).setAction("heartbeat"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
