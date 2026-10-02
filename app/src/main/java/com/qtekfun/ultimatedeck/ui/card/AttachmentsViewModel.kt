// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtekfun.ultimatedeck.data.attachments.AttachmentFiles
import com.qtekfun.ultimatedeck.data.attachments.AttachmentRepository
import com.qtekfun.ultimatedeck.data.local.entity.AttachmentEntity
import com.qtekfun.ultimatedeck.data.remote.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What happened to a request to open an attachment. */
sealed interface OpenResult {
    data class Ready(val file: File, val mimeType: String?) : OpenResult

    data object Failed : OpenResult
}

/** Attachments of the card shown in the detail (RF-07). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val repository: AttachmentRepository,
    val files: AttachmentFiles
) : ViewModel() {
    private val cardId = MutableStateFlow<Long?>(null)
    private val mutableOpening = MutableStateFlow<Set<Long>>(emptySet())
    private val mutableOpened = MutableSharedFlow<OpenResult>(extraBufferCapacity = 1)

    val attachments: StateFlow<List<AttachmentEntity>> = cardId.filterNotNull()
        .flatMapLatest(repository::observe)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Attachments being downloaded to be opened. */
    val opening: StateFlow<Set<Long>> = mutableOpening.asStateFlow()

    /** Files ready to hand to another app, or a failure to report. */
    val opened: SharedFlow<OpenResult> = mutableOpened.asSharedFlow()

    /** Shows [id]'s attachments and brings the server's list when there is network. */
    fun open(id: Long) {
        if (cardId.value == id) return
        cardId.value = id
        viewModelScope.launch { repository.refresh(id) }
    }

    fun add(uri: Uri) {
        val id = cardId.value ?: return
        viewModelScope.launch { repository.add(id, uri) }
    }

    fun view(attachment: AttachmentEntity) {
        mutableOpening.update { it + attachment.id }
        viewModelScope.launch {
            val result = repository.open(attachment)
            mutableOpening.update { it - attachment.id }
            mutableOpened.tryEmit(
                if (result is ApiResult.Success) {
                    OpenResult.Ready(
                        result.value,
                        attachment.mimeType
                    )
                } else {
                    OpenResult.Failed
                }
            )
        }
    }

    fun retry(attachment: AttachmentEntity) {
        viewModelScope.launch { repository.retry(attachment) }
    }

    fun discard(attachment: AttachmentEntity) {
        viewModelScope.launch { repository.discard(attachment) }
    }
}
