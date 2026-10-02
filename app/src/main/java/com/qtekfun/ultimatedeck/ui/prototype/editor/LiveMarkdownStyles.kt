// SPDX-FileCopyrightText: 2026 UltimateDeck contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatedeck.ui.prototype.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.qtekfun.ultimatedeck.domain.editor.InlineKind

/** Span styles the live markdown editor applies on top of the source text. */
internal data class LiveMarkdownStyles(
    val marker: SpanStyle,
    val strong: SpanStyle,
    val emphasis: SpanStyle,
    val strikethrough: SpanStyle,
    val code: SpanStyle,
    val link: SpanStyle,
    val monospaceBlock: SpanStyle,
    val checkbox: SpanStyle,
    val headingSizes: List<TextUnit>
) {
    fun inline(kind: InlineKind): SpanStyle = when (kind) {
        InlineKind.STRONG -> strong
        InlineKind.EMPHASIS -> emphasis
        InlineKind.STRIKETHROUGH -> strikethrough
        InlineKind.CODE -> code
        InlineKind.LINK -> link
    }

    fun heading(level: Int): SpanStyle = SpanStyle(
        fontSize = headingSizes[(level - 1).coerceIn(0, headingSizes.lastIndex)],
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun rememberLiveMarkdownStyles(): LiveMarkdownStyles {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    return remember(colors, typography) {
        LiveMarkdownStyles(
            marker = SpanStyle(color = colors.onSurfaceVariant.copy(alpha = MARKER_ALPHA)),
            strong = SpanStyle(fontWeight = FontWeight.Bold),
            emphasis = SpanStyle(fontStyle = FontStyle.Italic),
            strikethrough = SpanStyle(textDecoration = TextDecoration.LineThrough),
            code = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = colors.surfaceContainerHighest
            ),
            link = SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline),
            monospaceBlock = SpanStyle(
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
                background = colors.surfaceContainerHigh
            ),
            checkbox = SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold),
            headingSizes = listOf(
                typography.headlineSmall.fontSize,
                typography.titleLarge.fontSize,
                typography.titleMedium.fontSize
            )
        )
    }
}

private const val MARKER_ALPHA = 0.5f
