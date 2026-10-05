// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

/**
 * The periodic work that keeps reminders deliverable (RF-10): robust mode's service runs it
 * every few minutes. It never touches the network.
 */
fun interface ReminderBeat {
    suspend fun beat()
}
