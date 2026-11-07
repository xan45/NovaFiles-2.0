package com.novafiles.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation and card shadow tokens matching the reference design (0dp, 2dp, 4dp, 8dp, 12dp).
 */
@Immutable
data class NovaElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 2.dp,
    val level2: Dp = 4.dp,
    val level3: Dp = 8.dp,
    val level4: Dp = 12.dp
)

val LocalNovaElevation = staticCompositionLocalOf { NovaElevation() }
