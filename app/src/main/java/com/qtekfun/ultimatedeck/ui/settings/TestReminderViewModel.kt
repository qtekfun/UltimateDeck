// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.domain.reminders.TestDelivery
import com.qtekfun.ultimatedeck.notify.TestReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REFRESH_MS = 5_000L

/** The reminders test (RF-10): schedules it and keeps its state fresh while it is shown. */
@HiltViewModel
class TestReminderViewModel @Inject constructor(private val test: TestReminder) : ViewModel() {
    private val mutableDelivery = MutableStateFlow(test.delivery())
    val delivery: StateFlow<TestDelivery?> = mutableDelivery.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                mutableDelivery.value = test.delivery()
                delay(REFRESH_MS)
            }
        }
    }

    fun start(title: String) {
        viewModelScope.launch {
            test.schedule(title)
            mutableDelivery.value = test.delivery()
        }
    }
}
