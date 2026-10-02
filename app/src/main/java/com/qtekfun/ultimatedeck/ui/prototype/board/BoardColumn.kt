// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.qtekfun.ultimatedeck.R

private val CardShape = RoundedCornerShape(12.dp)

/**
 * One board column. It reports its own and its cards' bounds to [dragState] so the board can
 * resolve drop targets, and hides the dragged card behind a placeholder.
 */
@Composable
fun BoardColumn(
    column: PrototypeColumn,
    columnIndex: Int,
    columnTitles: List<String>,
    dragState: BoardDragState,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    DisposableEffect(columnIndex, listState) {
        dragState.columnListStates[columnIndex] = listState
        onDispose {
            dragState.columnListStates.remove(columnIndex)
            dragState.columnBounds.remove(columnIndex)
        }
    }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { dragState.columnBounds[columnIndex] = it.rootBounds() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            ColumnHeader(column.title, column.cards.size)
            LazyColumn(
                state = listState,
                userScrollEnabled = !dragState.isDragging,
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(column.cards, key = { it.id }) { card ->
                    BoardCard(
                        card = card,
                        isPlaceholder = dragState.dragged?.card?.id == card.id,
                        actions = moveActions(
                            card.id,
                            columnIndex,
                            columnTitles,
                            callbacks.onMoveToColumn
                        ),
                        onOpen = { callbacks.onOpen(card.id) },
                        dragState = dragState,
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "add") {
                    TextButton(
                        onClick = { callbacks.onAddCard(column.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.card_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(title: String, count: Int) {
    val description =
        pluralStringResource(R.plurals.prototype_column_description, count, title, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clearAndSetSemantics {
                heading()
                contentDescription = description
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun BoardCard(
    card: PrototypeCard,
    isPlaceholder: Boolean,
    actions: List<CustomAccessibilityAction>,
    onOpen: () -> Unit,
    dragState: BoardDragState,
    modifier: Modifier = Modifier
) {
    DisposableEffect(card.id) {
        onDispose { dragState.cardBounds.remove(card.id) }
    }
    val tracked = modifier.onGloballyPositioned { dragState.cardBounds[card.id] = it.rootBounds() }
    if (isPlaceholder) {
        val height = dragState.dragged?.size?.height ?: 0f
        val heightDp = with(LocalDensity.current) { height.toDp() }
        Box(
            modifier = tracked
                .fillMaxWidth()
                .height(heightDp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CardShape)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CardShape)
        )
    } else {
        PrototypeCardItem(card = card, actions = actions, onClick = onOpen, modifier = tracked)
    }
}

@Composable
private fun moveActions(
    cardId: Long,
    columnIndex: Int,
    columnTitles: List<String>,
    onMoveToColumn: (cardId: Long, column: Int) -> Unit
): List<CustomAccessibilityAction> = listOf(columnIndex - 1, columnIndex + 1)
    .filter { it in columnTitles.indices }
    .map { target ->
        val label = stringResource(R.string.prototype_action_move_to, columnTitles[target])
        CustomAccessibilityAction(label) {
            onMoveToColumn(cardId, target)
            true
        }
    }

private fun LayoutCoordinates.rootBounds(): Rect = Rect(positionInRoot(), size.toSize())
