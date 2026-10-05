// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import com.qtekfun.ultimatedeck.domain.reminders.isTest
import com.qtekfun.ultimatedeck.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL = "due_dates"

/** Shows a due date reminder; tapping it opens the card (RF-10). */
@Singleton
class ReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Shows [reminder], flagged as [missed] when it comes late. False when it could not be shown
     * because notifications are not allowed.
     */
    fun show(reminder: Reminder, missed: Boolean): Boolean {
        // Before Android 13 notifications need no permission.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        createChannel()
        val link = CardLink(reminder.boardId, reminder.boardTitle, reminder.cardId)
        val opening = Intent(context, MainActivity::class.java)
        val open = PendingIntent.getActivity(
            context,
            reminder.cardId.hashCode(),
            // The test reminder has no card to open.
            (if (reminder.isTest) opening else link.putInto(opening))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (reminder.isTest) {
            context.getString(R.string.reminder_test_text)
        } else {
            context.getString(
                if (missed) R.string.reminder_missed_text else R.string.reminder_text,
                reminder.boardTitle,
                formatDue(reminder)
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(reminder.cardId.hashCode(), notification)
        return true
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatDue(reminder: Reminder) = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.SHORT
    ).format(reminder.dueDate.atZone(ZoneId.systemDefault()))
}
