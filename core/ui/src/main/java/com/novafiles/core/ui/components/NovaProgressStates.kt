package com.novafiles.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.novafiles.core.designsystem.theme.NovaTheme

@Composable
fun NovaLinearProgress(
    progress: Float, // 0.0f to 1.0f
    title: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary
            )
            Text(
                text = "${(animatedProgress.value * 100).toInt()}%",
                style = NovaTheme.typography.captionMedium,
                color = NovaTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(NovaTheme.shapes.pill)
                .background(NovaTheme.colors.surfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .height(8.dp)
                    .clip(NovaTheme.shapes.pill)
                    .background(
                        Brush.horizontalGradient(
                            listOf(NovaTheme.colors.primary, NovaTheme.colors.secondary)
                        )
                    )
            )
        }
    }
}

@Composable
fun NovaEmptyState(
    title: String = "No files found",
    subtitle: String = "Try a different location or check permissions",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = NovaTheme.colors.textTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = NovaTheme.typography.title,
            color = NovaTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = NovaTheme.typography.body,
            color = NovaTheme.colors.textSecondary
        )
    }
}

@Composable
fun NovaErrorState(
    message: String = "Something went wrong",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = NovaTheme.colors.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = NovaTheme.typography.subtitle,
            color = NovaTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        NovaButton(
            text = "Tap to retry",
            icon = Icons.Default.Refresh,
            onClick = onRetry,
            variant = NovaButtonVariant.SECONDARY
        )
    }
}

@Composable
fun NovaLoadingState(
    modifier: Modifier = Modifier,
    text: String = "Loading..."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = NovaTheme.colors.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = NovaTheme.typography.body,
            color = NovaTheme.colors.textSecondary
        )
    }
}
