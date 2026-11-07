package com.novafiles.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

enum class NovaButtonVariant {
    PRIMARY,
    SECONDARY,
    GRADIENT,
    OUTLINE,
    TEXT
}

@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NovaButtonVariant = NovaButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = NovaTheme.shapes.pill,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
) {
    val alpha = if (enabled) 1f else 0.4f
    val colors = NovaTheme.colors

    val (bgModifier, textColor, border) = when (variant) {
        NovaButtonVariant.PRIMARY -> Triple(
            Modifier.background(colors.primary.copy(alpha = alpha), shape),
            Color.White,
            null
        )
        NovaButtonVariant.GRADIENT -> Triple(
            Modifier.background(
                Brush.horizontalGradient(
                    listOf(colors.primary.copy(alpha = alpha), colors.secondary.copy(alpha = alpha))
                ),
                shape
            ),
            Color.White,
            null
        )
        NovaButtonVariant.SECONDARY -> Triple(
            Modifier.background(colors.surfaceElevated.copy(alpha = alpha), shape),
            colors.textPrimary,
            BorderStroke(1.dp, colors.cardBorder)
        )
        NovaButtonVariant.OUTLINE -> Triple(
            Modifier.background(Color.Transparent, shape),
            colors.primary,
            BorderStroke(1.5.dp, colors.primary.copy(alpha = alpha))
        )
        NovaButtonVariant.TEXT -> Triple(
            Modifier.background(Color.Transparent, shape),
            colors.primary,
            null
        )
    }

    val clickModifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (border != null) Modifier.background(border.brush, shape) else Modifier)
            .then(bgModifier)
            .then(clickModifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = NovaTheme.typography.button,
                color = textColor
            )
        }
    }
}

@Composable
fun NovaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = NovaTheme.colors.textPrimary,
    backgroundColor: Color = NovaTheme.colors.surfaceElevated,
    borderColor: Color = NovaTheme.colors.cardBorder,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
