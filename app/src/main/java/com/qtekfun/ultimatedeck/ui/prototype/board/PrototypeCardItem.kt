// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtekfun.ultimatedeck.R
import com.qtekfun.ultimatedeck.ui.theme.UltimateDeckTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val PreviewDate: LocalDate = LocalDate.parse("2026-10-01")

/** A board card as seen at a glance: labels, title, due date, attachments, checklist, people. */
@Composable
fun PrototypeCardItem(
    card: PrototypeCard,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    actions: List<CustomAccessibilityAction> = emptyList()
) {
    val description = cardDescription(card, today)
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                customActions = actions
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (card.labels.isNotEmpty()) LabelRow(card.labels)
            Text(
                text = stringResource(card.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            CardFooter(card, today)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelRow(labels: List<PrototypeLabel>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEach { label ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(label.color, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(label.name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(card: PrototypeCard, today: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Metadata wraps to a new line instead of squeezing the avatars.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            card.dueDate?.let { due ->
                val color = if (due.isBefore(today)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                FooterItem(
                    rememberVectorPainter(Icons.Outlined.DateRange),
                    formatDate(due),
                    color
                )
            }
            if (card.attachments > 0) {
                FooterItem(painterResource(R.drawable.ic_attachment), card.attachments.toString())
            }
            if (card.checklistTotal > 0) {
                val colors = MaterialTheme.colorScheme
                val complete = card.checklistDone == card.checklistTotal
                FooterItem(
                    rememberVectorPainter(Icons.Outlined.CheckCircle),
                    "${card.checklistDone}/${card.checklistTotal}",
                    if (complete) colors.primary else colors.onSurfaceVariant
                )
            }
        }
        if (card.assignees.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            CardAvatars(card.assignees)
        }
    }
}

@Composable
private fun FooterItem(
    icon: Painter,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Due dates always show the year (SPEC.md, RF-03). */
private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
private fun cardDescription(card: PrototypeCard, today: LocalDate): String {
    val parts = buildList {
        add(stringResource(card.title))
        if (card.labels.isNotEmpty()) {
            val names = card.labels.map { stringResource(it.name) }.joinToString()
            add(stringResource(R.string.prototype_card_labels, names))
        }
        card.dueDate?.let {
            val res = if (it.isBefore(
                    today
                )
            ) {
                R.string.prototype_card_overdue
            } else {
                R.string.prototype_card_due
            }
            add(stringResource(res, formatDate(it)))
        }
        if (card.attachments > 0) {
            add(
                pluralStringResource(
                    R.plurals.prototype_card_attachments,
                    card.attachments,
                    card.attachments
                )
            )
        }
        if (card.checklistTotal > 0) {
            add(
                pluralStringResource(
                    R.plurals.prototype_card_checklist,
                    card.checklistTotal,
                    card.checklistDone,
                    card.checklistTotal
                )
            )
        }
        if (card.assignees.isNotEmpty()) {
            add(stringResource(R.string.prototype_card_assignees, card.assignees.joinToString()))
        }
    }
    return parts.joinToString(". ")
}

@Preview(showBackground = true)
@Composable
private fun PrototypeCardItemPreview() {
    val today = PreviewDate
    UltimateDeckTheme {
        PrototypeCardItem(
            card = FakeBoard.columns(today)[1].cards.first(),
            today = today,
            modifier = Modifier.padding(16.dp)
        )
    }
}
