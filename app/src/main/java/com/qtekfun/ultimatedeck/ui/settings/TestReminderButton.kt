// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.reminders.TestDelivery

/** Sends a real reminder a minute from now and says whether it arrived on time (RF-10). */
@Composable
fun TestReminderButton(viewModel: TestReminderViewModel = viewModel()) {
    val delivery by viewModel.delivery.collectAsStateWithLifecycle()
    val title = stringResource(R.string.reminder_test_title)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextButton(onClick = { viewModel.start(title) }) {
            Text(stringResource(R.string.settings_reminders_test))
        }
        delivery?.let {
            Text(
                testStatus(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (it is TestDelivery.OnTime) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun testStatus(delivery: TestDelivery): String = when (delivery) {
    is TestDelivery.Waiting -> stringResource(R.string.settings_reminders_test_waiting)

    is TestDelivery.OnTime -> stringResource(R.string.settings_reminders_test_on_time)

    is TestDelivery.Late ->
        stringResource(R.string.settings_reminders_test_late, delivery.minutes)

    is TestDelivery.Missing -> stringResource(R.string.settings_reminders_test_missing)
}
