// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import com.qtekfun.ultimatedeck.ui.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val CHANNEL = "due_dates"
private const val EXTRA_TITLE = "title"
private const val EXTRA_DUE = "due"

/** Shows a due date reminder; tapping it opens the card (RF-10). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val link = CardLink.from(intent) ?: return
        // Before Android 13 notifications need no permission.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        createChannel(context)
        val due = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_DUE, 0))
        val open = PendingIntent.getActivity(
            context,
            link.cardId.hashCode(),
            link.putInto(Intent(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE))
            .setContentText(
                context.getString(R.string.reminder_text, link.boardTitle, formatDue(due))
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(link.cardId.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatDue(due: Instant) = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.SHORT
    ).format(due.atZone(ZoneId.systemDefault()))

    companion object {
        /** What the notification needs, carried by the alarm. */
        fun describe(intent: Intent, reminder: Reminder) {
            CardLink(reminder.boardId, reminder.boardTitle, reminder.cardId).putInto(intent)
            intent.putExtra(
                EXTRA_TITLE,
                reminder.title
            ).putExtra(EXTRA_DUE, reminder.dueDate.toEpochMilli())
        }
    }
}
