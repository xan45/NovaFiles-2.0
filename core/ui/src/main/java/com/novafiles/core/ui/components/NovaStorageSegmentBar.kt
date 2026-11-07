package com.novafiles.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

data class StorageSegment(
    val color: Color,
    val weight: Float
)

@Composable
fun NovaStorageSegmentBar(
    segments: List<StorageSegment>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color = NovaTheme.colors.surfaceElevated
) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(NovaTheme.shapes.pill)
            .background(trackColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(progressAnim.value)
                .fillMaxHeight()
        ) {
            segments.forEach { segment ->
                if (segment.weight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(segment.weight / totalWeight)
                            .fillMaxHeight()
                            .background(segment.color)
                    )
                }
            }
        }
    }
}
