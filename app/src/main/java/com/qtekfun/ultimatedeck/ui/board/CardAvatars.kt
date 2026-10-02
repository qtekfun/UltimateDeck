// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_AVATARS = 3
private val AvatarSize = 24.dp
private val AvatarSpacing = 2.dp
private val AvatarTextSize = 10.sp

/** Assignee bubbles with initials; at most [MAX_AVATARS], then a "+N" bubble. */
@Composable
fun CardAvatars(names: List<String>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val containers = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer
    )
    val shown = names.take(MAX_AVATARS)
    val hidden = names.size - shown.size
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AvatarSpacing)) {
        shown.forEachIndexed { index, name ->
            val (container, content) = containers[index % containers.size]
            AvatarBubble(initials(name), container, content)
        }
        if (hidden > 0) {
            AvatarBubble("+$hidden", colors.surfaceContainerHighest, colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun AvatarBubble(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = AvatarTextSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = content
        )
    }
}

private fun initials(name: String): String = name.split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
