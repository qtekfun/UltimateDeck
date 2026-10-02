// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.domain.editor.DocumentSegment
import com.qtekfun.ultimatedeck.domain.editor.DocumentSegments
import com.qtekfun.ultimatedeck.domain.editor.MarkdownDocument
import com.qtekfun.ultimatedeck.domain.editor.MarkdownTable
import com.qtekfun.ultimatedeck.domain.editor.SegmentKind

/**
 * A description split into segments, each shown with its own editor: rendered text, table
 * grid, code or verbatim. Only the segment being edited is rewritten in the source.
 */
@Composable
fun MarkdownBlocks(
    source: String,
    editable: Boolean,
    callbacks: MarkdownBlocksCallbacks,
    modifier: Modifier = Modifier
) {
    val segments = remember(source) { DocumentSegments.split(MarkdownDocument.parse(source)) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        segments.forEachIndexed { index, segment ->
            val text = source.substring(segment.range.start, segment.range.end)
            val visible = text.isNotBlank() || (editable && index == segments.lastIndex)
            if (visible) {
                key(index, segment.kind) {
                    Segment(segment, text, editable, callbacks) { edited ->
                        callbacks.onSourceChange(DocumentSegments.replace(source, segment, edited))
                    }
                }
            }
        }
        // An empty description has no segments: offer a field to start writing.
        if (editable && segments.isEmpty()) {
            TextSegmentEditor(
                source,
                callbacks.onSourceChange,
                callbacks.onActiveText,
                Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Segment(
    segment: DocumentSegment,
    text: String,
    editable: Boolean,
    callbacks: MarkdownBlocksCallbacks,
    replace: (String) -> Unit
) {
    when (segment.kind) {
        SegmentKind.TEXT -> {
            // Blank lines around the text are kept in the source but not shown.
            val core = text.trim('\n', '\r')
            val before = text.substring(0, text.indexOf(core))
            val after = text.substring(before.length + core.length)
            val replaceCore: (String) -> Unit = { replace(before + it + after) }
            if (editable) {
                TextSegmentEditor(
                    core,
                    replaceCore,
                    callbacks.onActiveText,
                    Modifier.fillMaxWidth()
                )
            } else {
                MarkdownText(core, Modifier.fillMaxWidth(), replaceCore, callbacks.onTap)
            }
        }

        SegmentKind.TABLE -> TableGrid(
            table = remember(text) { MarkdownTable.parse(text) },
            editable = editable,
            onChange = { replace(it.serialize()) },
            onActive = callbacks.onActiveText
        )

        SegmentKind.CODE -> CodeSegment(text, editable, replace)

        SegmentKind.RAW -> RawSegment(text, editable, replace)
    }
}
