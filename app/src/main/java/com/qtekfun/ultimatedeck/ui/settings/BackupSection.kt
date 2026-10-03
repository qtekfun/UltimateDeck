// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R

/** Shortest password accepted to seal sessions in a backup. */
private const val MIN_PASSWORD = 8

/** Settings → Backup: export to a file and restore from one (T18d). */
@Composable
fun BackupSection(viewModel: BackupViewModel = viewModel()) {
    var exporting by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf<CharArray?>(null) }
    val create =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let { viewModel.export(it, password) }
            password = null
        }
    val fileName = stringResource(R.string.backup_file_name)
    Section(R.string.settings_backup)
    Row(Modifier.padding(horizontal = 8.dp)) {
        TextButton(onClick = { exporting = true }) { Text(stringResource(R.string.backup_export)) }
        RestoreBackupButton(viewModel)
    }
    if (exporting) {
        ExportDialog(
            onExport = { secret ->
                password = secret
                exporting = false
                create.launch(fileName)
            },
            onDismiss = { exporting = false }
        )
    }
}

/** "Restore a backup", also offered on the login screen of a new phone. */
@Composable
fun RestoreBackupButton(viewModel: BackupViewModel = viewModel()) {
    val context = LocalContext.current
    val waiting by viewModel.needsPassword.collectAsStateWithLifecycle()
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::startRestore)
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    TextButton(onClick = { open.launch(arrayOf("application/json", "*/*")) }) {
        Text(stringResource(R.string.backup_restore))
    }
    if (waiting != null) {
        PasswordDialog(
            onConfirm = viewModel::finishRestore,
            onSkip = { viewModel.finishRestore(null) },
            onDismiss = viewModel::cancelRestore
        )
    }
}

@Composable
private fun ExportDialog(onExport: (password: CharArray?) -> Unit, onDismiss: () -> Unit) {
    var withSessions by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val valid = !withSessions || (first.length >= MIN_PASSWORD && first == second)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.toggleable(withSessions, role = Role.Checkbox) { withSessions = it },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = withSessions, onCheckedChange = null)
                    Text(
                        stringResource(R.string.backup_include_sessions),
                        Modifier.padding(start = 8.dp)
                    )
                }
                if (withSessions) {
                    Text(
                        stringResource(R.string.backup_sessions_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    PasswordField(first, R.string.backup_password) { first = it }
                    PasswordField(second, R.string.backup_password_repeat) { second = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onExport(if (withSessions) first.toCharArray() else null)
            }, enabled = valid) {
                Text(stringResource(R.string.backup_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun PasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_restore_password_text))
                PasswordField(password, R.string.backup_password) { password = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(password.toCharArray())
            }, enabled = password.isNotEmpty()) {
                Text(stringResource(R.string.backup_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.backup_skip_sessions)) }
        }
    )
}

@Composable
private fun PasswordField(value: String, label: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}
