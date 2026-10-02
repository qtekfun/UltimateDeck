// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.board.CardPosition
import com.qtekfun.ultimatedeck.ui.prototype.remote.SyncProblemBanner
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Share of the screen width taken by one column; the next one peeks in from the edge. */
private const val COLUMN_WIDTH_FRACTION = 0.82f

/** Columns stop growing past this width so landscape still shows more than one. */
private val ColumnMaxWidth = 400.dp

/** Edge band, as a share of the board width, that triggers horizontal auto-scroll. */
private const val AUTO_SCROLL_EDGE_FRACTION = 0.15f
private val AutoScrollMaxSpeed = 14.dp
private val AutoScrollMinEdge = 48.dp

/**
 * Board view (T02) showing a real board loaded online (T06 preview): moves only change the copy
 * in memory until sync arrives (T09, T13).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardPrototypeScreen(
    title: String,
    viewModel: BoardPrototypeViewModel,
    onBack: () -> Unit,
    onOpenCard: (cardId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addingTo by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    ArchivedSnackbar(viewModel, snackbar)
    addingTo?.let { columnId ->
        AddCardDialog(
            onCreate = {
                viewModel.createCard(columnId, it)
                addingTo = null
            },
            onDismiss = { addingTo = null }
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { BoardTopBar(title, onBack) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                state.problem?.let { SyncProblemBanner(it) }
                when {
                    state.loading -> Unit

                    state.columns.isEmpty() -> Box(
                        // Scrollable so that the pull gesture also works on an empty board.
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.board_empty), textAlign = TextAlign.Center)
                    }

                    else -> Board(
                        columns = state.columns,
                        onMove = viewModel::moveCard,
                        callbacks = CardCallbacks(
                            viewModel::moveCardToColumn,
                            onOpenCard,
                            onAddCard = { addingTo = it }
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(title)
                Text(
                    text = stringResource(R.string.prototype_board_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.board_back)
                )
            }
        }
    )
}

/** "Card archived · Undo" after each archive. */
@Composable
private fun ArchivedSnackbar(viewModel: BoardPrototypeViewModel, snackbar: SnackbarHostState) {
    val message = stringResource(R.string.card_archived)
    val undo = stringResource(R.string.card_undo)
    LaunchedEffect(viewModel) {
        viewModel.archived.collect { cardId ->
            val result = snackbar.showSnackbar(message, undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoArchive(cardId)
        }
    }
}

@Composable
private fun Board(
    columns: List<PrototypeColumn>,
    onMove: (from: CardPosition, to: CardPosition) -> Unit,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier
) {
    val dragState = remember { BoardDragState() }
    val rowState = rememberLazyListState()
    val currentColumns = rememberUpdatedState(columns)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val columnTitles = columns.map { it.title }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { dragState.boardOrigin = it.positionInRoot() }
            .pointerInput(dragState) {
                detectCardDrag(
                    onStart = { position ->
                        dragState.start(position, currentColumns.value).also { started ->
                            if (started) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onMove = { dragState.moveTo(it, currentColumns.value) },
                    onDrop = {
                        dragState.drop()?.let { (from, to) ->
                            onMove(from, to)
                            scope.launch { rowState.animateScrollToItem(to.column) }
                        }
                    },
                    onCancel = dragState::cancel
                )
            }
    ) {
        val columnWidth = minOf(maxWidth * COLUMN_WIDTH_FRACTION, ColumnMaxWidth)
        AutoScroll(dragState, rowState, currentColumns, maxWidth)
        LazyRow(
            state = rowState,
            flingBehavior = rememberSnapFlingBehavior(rowState, SnapPosition.Start),
            userScrollEnabled = !dragState.isDragging,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(dragState.preview(columns), key = { _, column ->
                column.id
            }) { index, column ->
                BoardColumn(
                    column = column,
                    columnIndex = index,
                    columnTitles = columnTitles,
                    dragState = dragState,
                    callbacks = callbacks,
                    modifier = Modifier.width(columnWidth)
                )
            }
        }
        FloatingCard(dragState, LocalDensity.current.run { 1.dp.toPx() })
    }
}

@Composable
private fun FloatingCard(dragState: BoardDragState, dpPx: Float) {
    val dragged = dragState.dragged ?: return
    val width = with(LocalDensity.current) { dragged.size.width.toDp() }
    PrototypeCardItem(
        card = dragged.card,
        modifier = Modifier
            .width(width)
            .graphicsLayer {
                translationX = dragState.pointer.x - dragged.grabOffset.x
                translationY = dragState.pointer.y - dragged.grabOffset.y
                rotationZ = FLOATING_CARD_TILT
                scaleX = FLOATING_CARD_SCALE
                scaleY = FLOATING_CARD_SCALE
                shadowElevation = FLOATING_CARD_ELEVATION_DP * dpPx
            }
    )
}

private const val FLOATING_CARD_TILT = 2f
private const val FLOATING_CARD_SCALE = 1.04f
private const val FLOATING_CARD_ELEVATION_DP = 12f

/** Scrolls the board and the target column while the dragged card is near an edge. */
@Composable
private fun AutoScroll(
    dragState: BoardDragState,
    rowState: LazyListState,
    columns: State<List<PrototypeColumn>>,
    boardWidth: Dp
) {
    val density = LocalDensity.current
    LaunchedEffect(dragState.isDragging) {
        val widthPx = with(density) { boardWidth.toPx() }
        val edge =
            maxOf(widthPx * AUTO_SCROLL_EDGE_FRACTION, with(density) { AutoScrollMinEdge.toPx() })
        val maxSpeed = with(density) { AutoScrollMaxSpeed.toPx() }
        while (isActive && dragState.isDragging) {
            withFrameNanos { }
            val pointer = dragState.pointer
            val horizontal = autoScrollVelocity(pointer.x, 0f, widthPx, edge, maxSpeed)
            if (horizontal != 0f) rowState.scrollBy(horizontal)
            val vertical = dragState.target?.column?.let { column ->
                val bounds = dragState.columnBounds[column] ?: return@let 0f
                val top = bounds.top - dragState.boardOrigin.y
                val bottom = bounds.bottom - dragState.boardOrigin.y
                autoScrollVelocity(pointer.y, top, bottom, edge, maxSpeed).also { speed ->
                    if (speed != 0f) dragState.columnListStates[column]?.scrollBy(speed)
                }
            } ?: 0f
            if (horizontal != 0f || vertical != 0f) dragState.updateTarget(columns.value)
        }
    }
}

/**
 * Long-press-then-drag detector on the whole board. Once the long press fires, events are taken
 * in the Initial pass and consumed so the column lists underneath do not scroll.
 */
private suspend fun PointerInputScope.detectCardDrag(
    onStart: (Offset) -> Boolean,
    onMove: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
    if (!onStart(longPress.position)) return@awaitEachGesture
    var finished = false
    try {
        while (!finished) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                .firstOrNull { it.id == longPress.id }
            when {
                change == null -> onCancel()

                !change.pressed -> {
                    change.consume()
                    onDrop()
                }

                else -> {
                    onMove(change.position)
                    change.consume()
                }
            }
            finished = change == null || !change.pressed
        }
    } finally {
        if (!finished) onCancel()
    }
}
