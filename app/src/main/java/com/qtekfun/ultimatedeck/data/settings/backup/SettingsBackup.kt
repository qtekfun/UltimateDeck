// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings.backup

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.local.UltimateDeckDatabase
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.remote.ServerUrl
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.ReminderScope
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.data.settings.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Current backup format; older ones would be upgraded when read. */
private const val FORMAT = 1

/** The backup file: settings, and optionally the sessions sealed with a password. */
@Serializable
data class BackupFile(
    val format: Int = FORMAT,
    val settings: BackupSettings,
    val sessions: Sealed? = null
)

@Serializable
data class BackupSettings(
    val theme: String,
    val amoled: Boolean,
    val dynamicColor: Boolean,
    val favoriteBoardId: Long? = null,
    val reminders: Boolean,
    val reminderLead: String,
    val reminderScope: String,
    val reminderAlarmClock: Boolean,
    val allowDeleting: Boolean,
    val robustMode: Boolean = false
)

@Serializable
data class BackupSession(val serverUrl: String, val loginName: String, val appPassword: String)

/** How a restore went. */
sealed interface RestoreResult {
    /** Settings applied; [sessions] signed in (0 when there were none or one was already). */
    data class Restored(val sessions: Int) : RestoreResult

    data object WrongPassword : RestoreResult

    data object Invalid : RestoreResult
}

/**
 * Exports and restores the app's settings (T18d), to move to a new phone. Sessions (app
 * passwords) are only included on request, sealed with the user's password: see BackupCrypto.
 */
class SettingsBackup @Inject constructor(
    private val settings: SettingsRepository,
    private val session: AccountSession,
    database: UltimateDeckDatabase,
    private val credentials: CredentialStore
) {
    private val accounts = database.accountDao()
    private val json = Json { ignoreUnknownKeys = true }

    /** The backup as JSON; with a [password], the signed-in sessions go inside, sealed. */
    suspend fun export(password: CharArray?): String {
        val current = settings.settings.first()
        val sealed = password?.let { secret ->
            val sessions = accounts.observeAll().first().mapNotNull { account ->
                credentials.load(account.id)?.let {
                    BackupSession(account.serverUrl, it.loginName, it.appPassword)
                }
            }
            BackupCrypto.seal(json.encodeToString(sessions).toByteArray(), secret)
        }
        return json.encodeToString(BackupFile(settings = current.toBackup(), sessions = sealed))
    }

    /** Whether [backup] carries sessions, so a password must be asked for; null if unreadable. */
    fun hasSessions(backup: String): Boolean? = parse(backup)?.let { it.sessions != null }

    /**
     * Applies the settings and, when there is no session on this phone yet, signs in with the
     * ones in the backup. A [password] is needed only for those.
     */
    suspend fun restore(backup: String, password: CharArray?): RestoreResult {
        val file = parse(backup) ?: return RestoreResult.Invalid
        return sessionsToSignIn(file, password)?.let { apply(file, it) }
            ?: RestoreResult.WrongPassword
    }

    private suspend fun apply(file: BackupFile, sessions: List<BackupSession>): RestoreResult {
        settings.restore(file.settings.toSettings())
        val count = sessions.count { saved ->
            val url = ServerUrl.parse(saved.serverUrl) as? ServerUrl.ParseResult.Valid
            url?.let { session.signIn(it.url, Credentials(saved.loginName, saved.appPassword)) } !=
                null
        }
        return RestoreResult.Restored(count)
    }

    /** The sessions to sign in with: none if not asked or already signed in; null on a wrong password. */
    private suspend fun sessionsToSignIn(
        file: BackupFile,
        password: CharArray?
    ): List<BackupSession>? {
        val sealed = file.sessions
        if (sealed == null || password == null ||
            session.activeAccount.first() != null
        ) {
            return emptyList()
        }
        return BackupCrypto.open(sealed, password)?.let {
            json.decodeFromString<List<BackupSession>>(it.decodeToString())
        }
    }

    private fun parse(backup: String): BackupFile? = try {
        json.decodeFromString<BackupFile>(backup).takeIf { it.format <= FORMAT }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun AppSettings.toBackup() = BackupSettings(
    theme = theme.name,
    amoled = amoled,
    dynamicColor = dynamicColor,
    favoriteBoardId = favoriteBoardId,
    reminders = reminders,
    reminderLead = reminderLead.name,
    reminderScope = reminderScope.name,
    reminderAlarmClock = reminderAlarmClock,
    allowDeleting = allowDeleting,
    robustMode = robustMode
)

private fun BackupSettings.toSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.theme,
        amoled = amoled,
        dynamicColor = dynamicColor,
        favoriteBoardId = favoriteBoardId,
        reminders = reminders,
        reminderLead =
            ReminderLead.entries.firstOrNull { it.name == reminderLead } ?: defaults.reminderLead,
        reminderScope =
            ReminderScope.entries.firstOrNull {
                it.name == reminderScope
            } ?: defaults.reminderScope,
        reminderAlarmClock = reminderAlarmClock,
        allowDeleting = allowDeleting,
        robustMode = robustMode
    )
}
