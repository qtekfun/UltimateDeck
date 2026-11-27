// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.session.LogoutAction

/** Who is signed in, and signing out. */
@Composable
internal fun AccountSection(accountName: String, server: String, onLogOut: () -> Unit) {
    Section(R.string.settings_account)
    Text(
        accountName,
        Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodyLarge
    )
    Text(
        server,
        Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(Modifier.padding(horizontal = 8.dp)) { LogoutAction(accountName, onLogOut) }
}
