// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.notify

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.domain.reminders.ReminderPlanner
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the reminders in step with the cards and the settings (RF-10): any change to either
 * (sync, edit, archive, done, settings) plans and schedules them again.
 */
@Singleton
class ReminderCoordinator @Inject constructor(
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val clock: Clock
) {
    private val dao = database.reminderDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(session.activeAccount, settings.settings) { account, settings ->
                account to settings
            }
                .flatMapLatest { (account, settings) -> planned(account, settings) }
                .collect { (reminders, alarmClock) -> scheduler.schedule(reminders, alarmClock) }
        }
    }

    /** Sets every reminder again from the current cards and settings (robust mode's beat). */
    suspend fun replan() {
        val settings = settings.settings.first()
        val (reminders, alarmClock) = planned(session.activeAccount.first(), settings).first()
        scheduler.schedule(reminders, alarmClock)
    }

    private fun planned(account: AccountEntity?, settings: AppSettings) =
        if (account == null || !settings.reminders) {
            flowOf(emptyList())
        } else {
            dao.observeDueCards(account.id, account.userId)
        }.map { ReminderPlanner.plan(it, settings, clock.instant()) to settings.reminderAlarmClock }
}
