// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.card

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.domain.card.dueDateFor
import com.qtekfun.ultimatedeck.domain.card.pickerMillisFor
import com.qtekfun.ultimatedeck.domain.card.withTime
import com.qtekfun.ultimatedeck.ui.boards.deckColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Which metadata dialog is open. */
private enum class MetadataDialog { DATE, LABELS, MEMBERS }

/** Due date, labels and assignees of a card, each opening its own picker (T16). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardMetadata(
    card: CardDetail,
    onDueDate: (Instant?) -> Unit,
    onLabels: (Set<Long>) -> Unit,
    onAssignees: (Set<String>) -> Unit
) {
    var open by rememberSaveable { mutableStateOf<MetadataDialog?>(null) }
    val zone = remember { ZoneId.systemDefault() }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val overdue = card.dueDate?.isBefore(Instant.now()) == true
        val dueColor = if (overdue) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        AssistChip(
            onClick = { open = MetadataDialog.DATE },
            label = {
                Text(
                    card.dueDate?.let { formatDue(it, zone) }
                        ?: stringResource(R.string.card_due_add),
                    color = dueColor
                )
            },
            leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) }
        )
        AssistChip(
            onClick = { open = MetadataDialog.LABELS },
            label = { Text(labelSummary(card)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_label), contentDescription = null) }
        )
        AssistChip(
            onClick = { open = MetadataDialog.MEMBERS },
            label = { Text(memberSummary(card)) },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
        )
    }
    when (open) {
        MetadataDialog.DATE -> DueDateDialog(card.dueDate, zone, onDueDate) { open = null }

        MetadataDialog.LABELS -> ChoicesDialog(
            title = stringResource(R.string.card_labels),
            choices = card.labels.map { Choice(it.id, it.title, it.selected, it.color) },
            onConfirm = onLabels,
            onDismiss = { open = null }
        )

        MetadataDialog.MEMBERS -> ChoicesDialog(
            title = stringResource(R.string.card_assignees),
            choices = card.members.map { Choice(it.uid, it.name, it.selected) },
            onConfirm = onAssignees,
            onDismiss = { open = null }
        )

        null -> Unit
    }
}

@Composable
private fun labelSummary(card: CardDetail): String {
    val chosen = card.labels.filter { it.selected }
    return if (chosen.isEmpty()) {
        stringResource(R.string.card_labels)
    } else {
        chosen.joinToString {
            it.title
        }
    }
}

@Composable
private fun memberSummary(card: CardDetail): String {
    val chosen = card.members.filter { it.selected }
    return if (chosen.isEmpty()) {
        stringResource(R.string.card_assign)
    } else {
        chosen.joinToString {
            it.name
        }
    }
}

private fun formatDue(dueDate: Instant, zone: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(dueDate.atZone(zone))

/** Picks the day, then the time; the time starts at the current one (or midday). */
@Composable
private fun DueDateDialog(
    current: Instant?,
    zone: ZoneId,
    onDueDate: (Instant?) -> Unit,
    onDismiss: () -> Unit
) {
    var day by remember { mutableStateOf<Instant?>(null) }
    val picked = day
    if (picked == null) {
        DayDialog(current, zone, onPicked = { day = it }, onRemove = {
            onDueDate(null)
            onDismiss()
        }, onDismiss = onDismiss)
    } else {
        TimeDialog(picked, zone, onTime = {
            onDueDate(it)
            onDismiss()
        }, onDismiss = onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDialog(
    current: Instant?,
    zone: ZoneId,
    onPicked: (Instant) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = current?.let {
                pickerMillisFor(it, zone)
            }
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPicked(dueDateFor(it, current, zone)) }
                },
                enabled = state.selectedDateMillis != null
            ) { Text(stringResource(R.string.card_due_next)) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.card_due_remove))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            }
        }
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    day: Instant,
    zone: ZoneId,
    onTime: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val start = day.atZone(zone)
    val state = rememberTimePickerState(
        initialHour = start.hour,
        initialMinute = start.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.card_due_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onTime(withTime(day, state.hour, state.minute, zone)) }) {
                Text(stringResource(R.string.card_due_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

/** One option of a multiple choice; [color] draws a label dot. */
private data class Choice<T>(
    val id: T,
    val name: String,
    val selected: Boolean,
    val color: String? = null
)

@Composable
private fun <T> ChoicesDialog(
    title: String,
    choices: List<Choice<T>>,
    onConfirm: (Set<T>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember {
        mutableStateOf(choices.filter { it.selected }.map { it.id }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (choices.isEmpty()) Text(stringResource(R.string.card_choices_empty))
                choices.forEach { choice ->
                    val checked = choice.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(checked, role = Role.Checkbox) {
                                selected = if (it) selected + choice.id else selected - choice.id
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        choice.color?.let {
                            Box(Modifier.size(12.dp).background(deckColor(it), CircleShape))
                        }
                        Text(choice.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selected)
                onDismiss()
            }) { Text(stringResource(R.string.card_due_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}
