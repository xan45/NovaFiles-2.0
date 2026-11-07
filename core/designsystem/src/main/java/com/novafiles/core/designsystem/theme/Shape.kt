package com.novafiles.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape tokens matching the reference design corner radii (4dp, 8dp, 12dp, 16dp, 20dp, 28dp).
 */
@Immutable
data class NovaShapes(
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    val small: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val extraLarge: Shape = RoundedCornerShape(20.dp),
    val pill: Shape = RoundedCornerShape(28.dp)
)

val LocalNovaShapes = staticCompositionLocalOf { NovaShapes() }
