// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qtekfun.ultimatedeck.domain.reminders.Reminder
import com.qtekfun.ultimatedeck.domain.reminders.isTest
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val EXTRA_TITLE = "title"
private const val EXTRA_DUE = "due"
private const val EXTRA_AT = "at"
private const val EXTRA_ACCOUNT = "account"

/**
 * Shows a due date reminder, notes that it was shown, and brings back any other that did not
 * come in time (RF-10).
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notifier: ReminderNotifier

    @Inject
    lateinit var recovery: MissedReminderRecovery

    @Inject
    lateinit var testReminder: TestReminder

    override fun onReceive(context: Context, intent: Intent) {
        val reminder = read(intent) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (reminder.isTest) {
                    testReminder.arrived()
                    notifier.show(reminder, missed = false)
                } else if (notifier.show(reminder, missed = false)) {
                    recovery.markShown(reminder)
                }
                recovery.recover()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** What the notification needs, carried by the alarm. */
        fun describe(intent: Intent, reminder: Reminder) {
            CardLink(reminder.boardId, reminder.boardTitle, reminder.cardId).putInto(intent)
            intent.putExtra(EXTRA_TITLE, reminder.title)
                .putExtra(EXTRA_DUE, reminder.dueDate.toEpochMilli())
                .putExtra(EXTRA_AT, reminder.at.toEpochMilli())
                .putExtra(EXTRA_ACCOUNT, reminder.accountId)
        }

        /** The reminder an alarm carries; null if it is not one of ours. */
        fun read(intent: Intent): Reminder? {
            val link = CardLink.from(intent) ?: return null
            val due = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_DUE, 0))
            return Reminder(
                accountId = intent.getLongExtra(EXTRA_ACCOUNT, NO_ACCOUNT),
                cardId = link.cardId,
                boardId = link.boardId,
                boardTitle = link.boardTitle,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                dueDate = due,
                // Alarms set by an earlier version carry no time of their own.
                at = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, due.toEpochMilli()))
            )
        }
    }
}
