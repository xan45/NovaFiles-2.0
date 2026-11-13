package com.novafiles.core.ui.branding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novafiles.core.designsystem.R
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.NovaTheme
import kotlinx.coroutines.delay

@Composable
fun NovaSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }
    var textVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    LaunchedEffect(Unit) {
        // Smooth entrance
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300)
        )
        textVisible = true
        delay(750) // Restrained, brief startup duration
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Central 3D Icon with Luminous Radial Halo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x665B6EF5),
                                    Color(0x408A6CFF),
                                    Color(0x202DD4BF),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = (size.width * 0.8f) * pulseGlow
                            ),
                            radius = (size.width * 0.8f) * pulseGlow
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_novafiles_3d),
                    contentDescription = "NovaFiles Logo",
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Wordmark and Tagline
            AnimatedVisibility(
                visible = textVisible,
                enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
                    initialOffsetY = { 30 },
                    animationSpec = tween(350)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NovaFiles",
                        style = NovaTheme.typography.display.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = NovaTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Advanced Storage & File Management Suite",
                        style = NovaTheme.typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = NovaTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
