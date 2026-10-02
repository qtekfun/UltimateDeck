// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.local.entity.UploadState

/** Attachments of a card: add from camera, gallery or files; open, retry or discard (RF-07). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentsSection(cardId: Long, viewModel: AttachmentsViewModel = viewModel()) {
    LaunchedEffect(cardId) { viewModel.open(cardId) }
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val opening by viewModel.opening.collectAsStateWithLifecycle()
    OpenedFiles(viewModel)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.attachments_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        attachments.forEach { attachment ->
            AttachmentRow(attachment, attachment.id in opening, viewModel)
        }
        AddAttachmentButtons(viewModel)
        if (attachments.isEmpty()) {
            Text(
                stringResource(R.string.attachments_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/** Camera, gallery and file pickers; whatever is picked is copied and queued for upload. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddAttachmentButtons(viewModel: AttachmentsViewModel) {
    var photo by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        photo?.takeIf { taken }?.let(viewModel::add)
    }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::add)
        }
    val document =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::add)
        }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {
                val target = viewModel.files.shareable(viewModel.files.newPhoto())
                photo = target
                camera.launch(target)
            },
            label = { Text(stringResource(R.string.attachments_camera)) }
        )
        AssistChip(
            onClick = {
                gallery.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )
            },
            label = { Text(stringResource(R.string.attachments_gallery)) }
        )
        AssistChip(
            onClick = { document.launch(arrayOf("*/*")) },
            label = { Text(stringResource(R.string.attachments_file)) }
        )
    }
}

/** Hands downloaded files to another app, or tells why it could not. */
@Composable
private fun OpenedFiles(viewModel: AttachmentsViewModel) {
    val context = LocalContext.current
    val failed = stringResource(R.string.attachments_open_failed)
    val noApp = stringResource(R.string.attachments_no_app)
    LaunchedEffect(viewModel) {
        viewModel.opened.collect { result ->
            when (result) {
                is OpenResult.Ready -> try {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                            viewModel.files.shareable(result.file),
                            result.mimeType ?: "*/*"
                        )
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, noApp, Toast.LENGTH_SHORT).show()
                }

                OpenResult.Failed -> Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: AttachmentEntity,
    opening: Boolean,
    viewModel: AttachmentsViewModel
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = attachment.uploadState == UploadState.DONE && !opening) {
                viewModel.view(attachment)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painterResource(R.drawable.ic_attachment), contentDescription = null)
        Column(Modifier.weight(1f)) {
            Text(attachment.fileName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${Formatter.formatShortFileSize(
                    context,
                    attachment.size
                )} · ${stringResource(stateLabel(attachment.uploadState))}",
                style = MaterialTheme.typography.bodySmall,
                color = if (attachment.uploadState == UploadState.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        when {
            opening || attachment.uploadState == UploadState.UPLOADING ->
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)

            attachment.uploadState == UploadState.FAILED -> {
                TextButton(onClick = {
                    viewModel.retry(attachment)
                }) { Text(stringResource(R.string.attachments_retry)) }
                TextButton(onClick = {
                    viewModel.discard(attachment)
                }) { Text(stringResource(R.string.attachments_discard)) }
            }

            else -> DeleteAttachmentButton(attachment.fileName) { viewModel.delete(attachment) }
        }
    }
}

/** Deleting an uploaded attachment removes it from the server too, so it is confirmed. */
@Composable
private fun DeleteAttachmentButton(name: String, onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    IconButton(onClick = { confirming = true }) {
        Icon(Icons.Filled.Delete, stringResource(R.string.attachments_delete, name))
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.attachments_delete_title)) },
            text = { Text(stringResource(R.string.attachments_delete_text, name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text(stringResource(R.string.card_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirming = false
                }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

private fun stateLabel(state: UploadState) = when (state) {
    UploadState.PENDING -> R.string.attachments_pending
    UploadState.UPLOADING -> R.string.attachments_uploading
    UploadState.FAILED -> R.string.attachments_failed
    UploadState.DONE -> R.string.attachments_done
}
