// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.editor.MarkdownTable
import com.qtekfun.ultimatedeck.domain.editor.MarkdownTable.Companion.HEADER_ROW

private val CellWidth = 140.dp
private val CellMinHeight = 48.dp
private val GridShape = RoundedCornerShape(8.dp)

/** A GFM table as a grid. When [editable], cells are rendered text fields and rows/columns can change. */
@Composable
fun TableGrid(
    table: MarkdownTable,
    editable: Boolean,
    onChange: (MarkdownTable) -> Unit,
    onActive: (TextCommandTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, GridShape)
        ) {
            (
                listOf(HEADER_ROW to table.header) +
                    table.rows.mapIndexed { index, row -> index to row }
                )
                .forEach { (rowIndex, cells) ->
                    Row {
                        cells.forEachIndexed { column, cell ->
                            TableCell(
                                text = cell,
                                header = rowIndex == HEADER_ROW,
                                editable = editable,
                                onChange = { onChange(table.withCell(rowIndex, column, it)) },
                                onActive = onActive
                            )
                        }
                    }
                }
        }
        if (editable) TableControls(table, onChange)
    }
}

@Composable
private fun TableCell(
    text: String,
    header: Boolean,
    editable: Boolean,
    onChange: (String) -> Unit,
    onActive: (TextCommandTarget) -> Unit
) {
    val background =
        with(MaterialTheme.colorScheme) { if (header) surfaceContainerHigh else surface }
    Box(
        modifier = Modifier
            .width(CellWidth)
            .heightIn(min = CellMinHeight)
            .background(background)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (editable) {
            TextSegmentEditor(
                text = text,
                onTextChange = onChange,
                onActive = onActive,
                singleLine = true
            )
        } else {
            MarkdownText(source = text)
        }
    }
}

@Composable
private fun TableControls(table: MarkdownTable, onChange: (MarkdownTable) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onChange(table.withRowAdded()) }) {
            Text(stringResource(R.string.table_add_row))
        }
        OutlinedButton(onClick = { onChange(table.withColumnAdded()) }) {
            Text(stringResource(R.string.table_add_column))
        }
        OutlinedButton(
            onClick = { onChange(table.withRowRemoved(table.rows.lastIndex)) },
            enabled = table.rows.isNotEmpty()
        ) { Text(stringResource(R.string.table_remove_row)) }
        OutlinedButton(
            onClick = { onChange(table.withColumnRemoved(table.columnCount - 1)) },
            enabled = table.columnCount > 1
        ) { Text(stringResource(R.string.table_remove_column)) }
    }
}
