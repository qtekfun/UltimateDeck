// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.session

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R

/** Top bar button that logs out after confirming, since local data of the account is deleted. */
@Composable
fun LogoutAction(accountName: String, onLogOut: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { confirming = true }) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        Text(stringResource(R.string.logout), Modifier.padding(start = 8.dp))
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_text, accountName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onLogOut()
                }) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirming = false
                }) { Text(stringResource(R.string.login_cancel)) }
            }
        )
    }
}
