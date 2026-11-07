package com.novafiles.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

enum class NovaCardVariant {
    STANDARD,
    GRADIENT,
    OUTLINE
}

@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    variant: NovaCardVariant = NovaCardVariant.STANDARD,
    shape: Shape = NovaTheme.shapes.large,
    elevation: Dp = NovaTheme.elevation.level1,
    backgroundColor: Color = NovaTheme.colors.surfaceElevated,
    borderColor: Color = NovaTheme.colors.cardBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    val backgroundModifier = when (variant) {
        NovaCardVariant.STANDARD -> Modifier.background(backgroundColor, shape)
        NovaCardVariant.GRADIENT -> Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    NovaTheme.colors.primary.copy(alpha = 0.85f),
                    NovaTheme.colors.secondary.copy(alpha = 0.85f)
                )
            ),
            shape = shape
        )
        NovaCardVariant.OUTLINE -> Modifier.background(Color.Transparent, shape)
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .then(backgroundModifier)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .then(clickModifier)
            .padding(16.dp),
        content = content
    )
}
