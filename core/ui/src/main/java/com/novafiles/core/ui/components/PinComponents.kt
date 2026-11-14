package com.novafiles.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novafiles.core.designsystem.theme.NovaTheme

/**
 * 4-dot indicator showing how many PIN digits have been entered.
 */
@Composable
fun PinDotsIndicator(
    enteredCount: Int,
    maxDigits: Int = 4,
    hasError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxDigits) { index ->
            val isFilled = index < enteredCount
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.15f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "dotScale"
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    hasError -> NovaTheme.colors.error
                    isFilled -> NovaTheme.colors.primary
                    else -> Color.Transparent
                },
                animationSpec = tween(durationMillis = 150),
                label = "dotColor"
            )
            val borderColor by animateColorAsState(
                targetValue = when {
                    hasError -> NovaTheme.colors.error
                    isFilled -> NovaTheme.colors.primary
                    else -> NovaTheme.colors.cardBorder
                },
                animationSpec = tween(durationMillis = 150),
                label = "dotBorderColor"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, borderColor, CircleShape)
            )
        }
    }
}

/**
 * Modern numeric keypad grid (1-9, 0, backspace).
 */
@Composable
fun PinKeypad(
    onDigitPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    isLockedOut: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    PinKeypadButton(
                        text = digit.toString(),
                        enabled = !isLockedOut,
                        onClick = { onDigitPress(digit) }
                    )
                }
            }
        }

        // Bottom row: Empty Spacer, '0', Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(68.dp))

            PinKeypadButton(
                text = "0",
                enabled = !isLockedOut,
                onClick = { onDigitPress('0') }
            )

            IconButton(
                onClick = onBackspace,
                enabled = !isLockedOut,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Backspace,
                    contentDescription = "Backspace",
                    tint = if (!isLockedOut) NovaTheme.colors.textSecondary else NovaTheme.colors.textSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun PinKeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                if (enabled) NovaTheme.colors.surfaceElevated else NovaTheme.colors.surfaceElevated.copy(alpha = 0.4f)
            )
            .border(
                1.dp,
                if (enabled) NovaTheme.colors.cardBorder else NovaTheme.colors.cardBorder.copy(alpha = 0.2f),
                CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) NovaTheme.colors.textPrimary else NovaTheme.colors.textSecondary.copy(alpha = 0.4f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp
        )
    }
}

/**
 * Reusable dialog for setting up or updating a 4-digit PIN.
 */
@Composable
fun PinSetupDialog(
    onDismissRequest: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    title: String = "Set App Lock PIN"
) {
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleDigit(d: Char) {
        errorMessage = null
        if (step == 1) {
            if (firstPin.length < 4) {
                val updated = firstPin + d
                firstPin = updated
                if (updated.length == 4) {
                    step = 2
                }
            }
        } else {
            if (confirmPin.length < 4) {
                val updated = confirmPin + d
                confirmPin = updated
                if (updated.length == 4) {
                    if (updated == firstPin) {
                        onPinConfirmed(updated)
                    } else {
                        errorMessage = "PINs do not match. Try again."
                        step = 1
                        firstPin = ""
                        confirmPin = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        errorMessage = null
        if (step == 1) {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = 1
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        NovaCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            backgroundColor = NovaTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                        .border(1.5.dp, NovaTheme.colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (step == 1) title else "Confirm PIN",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (step == 1) "Enter a 4-digit PIN" else "Re-enter the 4-digit PIN to confirm",
                    style = NovaTheme.typography.captionMedium,
                    color = NovaTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PinDotsIndicator(
                    enteredCount = if (step == 1) firstPin.length else confirmPin.length,
                    hasError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = NovaTheme.colors.error,
                        style = NovaTheme.typography.captionMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PinKeypad(
                    onDigitPress = { handleDigit(it) },
                    onBackspace = { handleBackspace() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NovaButton(
                    text = "Cancel",
                    onClick = onDismissRequest,
                    variant = NovaButtonVariant.TEXT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
