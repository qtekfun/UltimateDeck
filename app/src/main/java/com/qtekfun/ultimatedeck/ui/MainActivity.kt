// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatedeck.data.settings.AppSettings
import com.qtekfun.ultimatedeck.data.settings.SettingsRepository
import com.qtekfun.ultimatedeck.ui.prototype.PrototypeApp
import com.qtekfun.ultimatedeck.ui.theme.UltimateDeckTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
            UltimateDeckTheme(settings) {
                PrototypeApp()
            }
        }
    }
}
