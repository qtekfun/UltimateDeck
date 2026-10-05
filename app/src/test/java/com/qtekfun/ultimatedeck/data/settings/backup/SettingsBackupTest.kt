// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings.backup

import com.qtekfun.ultimatedeck.data.auth.AccountSession
import com.qtekfun.ultimatedeck.data.auth.CredentialStore
import com.qtekfun.ultimatedeck.data.local.entity.AccountEntity
import com.qtekfun.ultimatedeck.data.local.inMemoryDatabase
import com.qtekfun.ultimatedeck.data.remote.Credentials
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.FakePreferences
import com.qtekfun.ultimatedeck.data.settings.ReminderLead
import com.qtekfun.ultimatedeck.data.settings.SettingFlag
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.data.settings.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsBackupTest {
    private val db = inMemoryDatabase()
    private val oldPhone = SettingsRepository(FakePreferences())
    private val newPhone = SettingsRepository(FakePreferences())
    private val session = mockk<AccountSession>(relaxed = true)
    private val store = mockk<CredentialStore>()
    private val password = "correct horse".toCharArray()

    @AfterEach
    fun close() = db.close()

    private suspend fun exported(withSessions: Boolean): String {
        db.accountDao().insert(AccountEntity(1, "https://cloud.example/", "ana", "Ana"))
        coEvery { store.load(1) } returns Credentials("ana", "app-password")
        oldPhone.setTheme(ThemeMode.DARK)
        oldPhone.setFlag(SettingFlag.AMOLED, true)
        oldPhone.setFlag(SettingFlag.ROBUST_MODE, true)
        oldPhone.setReminderLead(ReminderLead.ONE_DAY)
        oldPhone.setFlag(SettingFlag.RECOVER_MISSED, false)
        oldPhone.setFavoriteBoard(19)
        return SettingsBackup(
            oldPhone,
            session,
            db,
            store
        ).export(if (withSessions) password else null)
    }

    private fun restorer(signedIn: Boolean): SettingsBackup {
        every { session.activeAccount } returns
            flowOf(if (signedIn) AccountEntity(2, "https://x/", "bob", "Bob") else null)
        return SettingsBackup(newPhone, session, inMemoryDatabase(), store)
    }

    @Test
    fun `settings move to a new phone without sessions unless asked`() = runTest {
        val backup = exported(withSessions = false)
        val restorer = restorer(signedIn = false)

        assertFalse(restorer.hasSessions(backup)!!)
        assertFalse("app-password" in backup)
        assertEquals(RestoreResult.Restored(0), restorer.restore(backup, null))
        assertEquals(
            AppSettings(
                theme = ThemeMode.DARK,
                amoled = true,
                robustMode = true,
                favoriteBoardId = 19,
                reminderLead = ReminderLead.ONE_DAY,
                recoverMissed = false
            ),
            newPhone.settings.first()
        )
        coVerify(exactly = 0) { session.signIn(any(), any()) }
    }

    @Test
    fun `sessions travel sealed and sign in on the new phone with the password`() = runTest {
        val backup = exported(withSessions = true)
        val restorer = restorer(signedIn = false)

        assertTrue(restorer.hasSessions(backup)!!)
        assertFalse("app-password" in backup)
        assertEquals(RestoreResult.Restored(1), restorer.restore(backup, password))
        coVerify {
            session.signIn(
                match { it.root.toString() == "https://cloud.example/" },
                match {
                    it.appPassword ==
                        "app-password"
                }
            )
        }
    }

    @Test
    fun `a backup from before missed reminders came back restores them as on`() = runTest {
        val backup = exported(withSessions = false).replace(",\"recoverMissed\":false", "")

        restorer(signedIn = false).restore(backup, null)

        assertTrue(newPhone.settings.first().recoverMissed)
    }

    @Test
    fun `a wrong password changes nothing`() = runTest {
        val backup = exported(withSessions = true)

        assertEquals(
            RestoreResult.WrongPassword,
            restorer(signedIn = false).restore(backup, "nope".toCharArray())
        )
        assertEquals(AppSettings(), newPhone.settings.first())
    }

    @Test
    fun `a phone already signed in keeps its session and only takes the settings`() = runTest {
        val backup = exported(withSessions = true)

        assertEquals(RestoreResult.Restored(0), restorer(signedIn = true).restore(backup, password))
        assertEquals(ThemeMode.DARK, newPhone.settings.first().theme)
        coVerify(exactly = 0) { session.signIn(any(), any()) }
    }

    @Test
    fun `other files are not backups`() = runTest {
        val restorer = restorer(signedIn = false)

        assertNull(restorer.hasSessions("hello"))
        assertNull(restorer.hasSessions("""{"format":99,"settings":{}}"""))
        assertEquals(RestoreResult.Invalid, restorer.restore("{}", null))
    }

    @Test
    fun `sealed data opens only with its password and unchanged`() {
        val sealed = BackupCrypto.seal("secret".toByteArray(), password)

        assertEquals("secret", BackupCrypto.open(sealed, password)?.decodeToString())
        assertNull(BackupCrypto.open(sealed, "other".toCharArray()))
        assertNull(
            BackupCrypto.open(
                sealed.copy(data = BackupCrypto.seal("x".toByteArray(), password).data),
                password
            )
        )
    }
}
