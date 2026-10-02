// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/** Languages the app can be set to; null tag = follow the system. */
enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), SPANISH("es") }

/** Per-app language (Android 13+), kept by the system itself; older versions follow the system. */
object AppLanguages {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun current(context: Context): AppLanguage {
        val tags = context.getSystemService(
            LocaleManager::class.java
        ).applicationLocales.toLanguageTags()
        return AppLanguage.entries.firstOrNull { it.tag != null && tags.startsWith(it.tag) }
            ?: AppLanguage.SYSTEM
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun set(context: Context, language: AppLanguage) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
    }
}
