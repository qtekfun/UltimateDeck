// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.data.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.qtekfun.ultimatedeck.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** A file copied into the app, with what the server needs to know about it. */
data class ImportedFile(val file: File, val name: String, val mimeType: String?)

/**
 * Where attachment files live (RF-07). Picked files are copied into the app, so an upload
 * survives restarts and the loss of the picker's permission; downloads are kept next to them.
 */
@Singleton
class AttachmentFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    private val resolver get() = context.contentResolver

    suspend fun import(uri: Uri, accountId: Long, attachmentId: Long): ImportedFile? =
        withContext(io) {
            val name = displayName(uri) ?: "attachment"
            val target = fileFor(accountId, attachmentId, name)
            val copied = resolver.openInputStream(uri)?.use { copy(it, target) }
            copied?.let { ImportedFile(it, name, resolver.getType(uri)) }
        }

    /** Where an attachment of [accountId] is kept on this device. */
    fun fileFor(accountId: Long, attachmentId: Long, name: String): File =
        File(context.filesDir, "attachments/$accountId/$attachmentId/${name.replace('/', '_')}")

    suspend fun save(input: InputStream, target: File): File = withContext(io) {
        copy(input, target)
    }

    /** Deletes a copy that is no longer needed. */
    suspend fun delete(path: String) = withContext(io) { File(path).delete() }

    private fun copy(input: InputStream, target: File): File {
        target.parentFile?.mkdirs()
        target.outputStream().use { input.copyTo(it) }
        return target
    }

    /** A new file for the camera to write a photo into. */
    fun newPhoto(): File = File(context.cacheDir, "camera/photo-${System.nanoTime()}.jpg").apply {
        parentFile?.mkdirs()
    }

    /** A content URI other apps can read (or the camera write) through the app's FileProvider. */
    fun shareable(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    private fun displayName(uri: Uri): String? = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}
