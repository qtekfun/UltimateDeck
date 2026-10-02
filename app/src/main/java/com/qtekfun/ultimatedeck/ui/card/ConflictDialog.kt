// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.local.model.CardField
import com.qtekfun.ultimatedeck.sync.conflict.TextConflict
import kotlinx.coroutines.launch

/**
 * A title or description changed here and on the server (SPEC §5): both versions side by side,
 * with a way to copy mine before deciding.
 */
@Composable
fun ConflictDialog(
    conflict: TextConflict,
    onResolve: (keepMine: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val field = if (conflict.field ==
        CardField.TITLE
    ) {
        R.string.card_field_title
    } else {
        R.string.card_field_description
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conflict_title, stringResource(field))) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.conflict_mine),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(conflict.local)
                TextButton(onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("card", conflict.local))
                        )
                    }
                }) { Text(stringResource(R.string.conflict_copy_mine)) }
                Text(
                    stringResource(R.string.conflict_server),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(conflict.server)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onResolve(true)
            }) { Text(stringResource(R.string.conflict_keep_mine)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onResolve(false)
            }) { Text(stringResource(R.string.conflict_use_server)) }
        }
    )
}
