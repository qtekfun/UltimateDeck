// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.settings.backup.RestoreResult
import com.qtekfun.ultimatedeck.data.settings.backup.SettingsBackup
import com.qtekfun.ultimatedeck.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Exports the settings to a file the user picks, and restores them from one (T18d). */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: SettingsBackup,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val mutableMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val pending = MutableStateFlow<String?>(null)

    /** Messages to show, as string resources. */
    val messages: SharedFlow<Int> = mutableMessages.asSharedFlow()

    /** A backup with sessions waiting for its password. */
    val needsPassword: StateFlow<String?> = pending.asStateFlow()

    fun export(target: Uri, password: CharArray?) {
        viewModelScope.launch {
            val written = withContext(io) {
                val text = backup.export(password)
                runCatchingIo {
                    context.contentResolver.openOutputStream(target)?.use {
                        it.write(text.toByteArray())
                    }
                }
            }
            mutableMessages.tryEmit(
                if (written !=
                    null
                ) {
                    R.string.backup_exported
                } else {
                    R.string.backup_failed
                }
            )
        }
    }

    fun startRestore(source: Uri) {
        viewModelScope.launch {
            val text = withContext(io) {
                runCatchingIo {
                    context.contentResolver.openInputStream(source)?.use {
                        it.readBytes().decodeToString()
                    }
                }
            }
            when (text?.let(backup::hasSessions)) {
                null -> mutableMessages.tryEmit(R.string.backup_invalid)
                true -> pending.value = text
                false -> finish(text, null)
            }
        }
    }

    /** Restores the waiting backup; a null [password] skips its sessions. */
    fun finishRestore(password: CharArray?) {
        val text = pending.value ?: return
        viewModelScope.launch { finish(text, password) }
    }

    fun cancelRestore() {
        pending.value = null
    }

    private suspend fun finish(text: String, password: CharArray?) {
        when (val result = backup.restore(text, password)) {
            is RestoreResult.Restored -> {
                pending.value = null
                mutableMessages.tryEmit(
                    if (result.sessions >
                        0
                    ) {
                        R.string.backup_restored_with_session
                    } else {
                        R.string.backup_restored
                    }
                )
            }

            RestoreResult.WrongPassword -> mutableMessages.tryEmit(R.string.backup_wrong_password)

            RestoreResult.Invalid -> {
                pending.value = null
                mutableMessages.tryEmit(R.string.backup_invalid)
            }
        }
    }

    private inline fun <T> runCatchingIo(block: () -> T?): T? = try {
        block()
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
