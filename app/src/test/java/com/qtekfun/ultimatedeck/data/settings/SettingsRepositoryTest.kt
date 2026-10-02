// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.settings

import android.content.SharedPreferences
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** In-memory SharedPreferences that notifies listeners like the real one. */
private class FakePreferences : SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = values
    override fun getString(key: String, defValue: String?) = values[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?) = defValues
    override fun getInt(key: String, defValue: Int) = defValue
    override fun getLong(key: String, defValue: Long) = defValue
    override fun getFloat(key: String, defValue: Float) = defValue
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String) = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners += listener
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners -= listener
    }

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()

        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = this
        override fun putInt(key: String, value: Int) = this
        override fun putLong(key: String, value: Long) = this
        override fun putFloat(key: String, value: Float) = this
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = this
        override fun clear() = this
        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            values += changes
            changes.keys.forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(this@FakePreferences, key) }
            }
        }
    }
}

class SettingsRepositoryTest {
    private val preferences = FakePreferences()
    private val repository = SettingsRepository(preferences)

    @Test
    fun `starts with the defaults and follows every change`() = runTest {
        repository.settings.test {
            assertEquals(AppSettings(), awaitItem())
            repository.setTheme(ThemeMode.DARK)
            assertEquals(AppSettings(theme = ThemeMode.DARK), awaitItem())
            repository.setAmoled(true)
            assertEquals(AppSettings(theme = ThemeMode.DARK, amoled = true), awaitItem())
            repository.setDynamicColor(false)
            assertEquals(
                AppSettings(ThemeMode.DARK, amoled = true, dynamicColor = false),
                awaitItem()
            )
        }
    }

    @Test
    fun `an unknown stored theme falls back to the system one`() = runTest {
        preferences.values["theme"] = "NEON"

        repository.settings.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().theme)
        }
    }
}
