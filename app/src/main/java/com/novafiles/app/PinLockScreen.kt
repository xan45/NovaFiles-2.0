package com.novafiles.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.security.PinSecurityManager
import com.novafiles.core.security.PinVerificationResult
import com.novafiles.core.ui.components.PinDotsIndicator
import com.novafiles.core.ui.components.PinKeypad
import kotlinx.coroutines.delay

/**
 * Fullscreen App Lock Screen shown on app launch when App Lock is enabled.
 */
@Composable
fun PinLockScreen(
    pinSecurityManager: PinSecurityManager,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember {
        mutableLongStateOf(pinSecurityManager.getRemainingLockoutSeconds())
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            while (lockoutSeconds > 0) {
                delay(1000)
                lockoutSeconds = pinSecurityManager.getRemainingLockoutSeconds()
            }
            errorMessage = null
        }
    }

    fun handleDigit(digit: Char) {
        if (lockoutSeconds > 0) return
        errorMessage = null
        if (enteredPin.length < 4) {
            val updated = enteredPin + digit
            enteredPin = updated
            if (updated.length == 4) {
                when (val result = pinSecurityManager.verifyPin(updated)) {
                    is PinVerificationResult.Success,
                    is PinVerificationResult.DecoySuccess -> {
                        enteredPin = ""
                        onUnlockSuccess()
                    }
                    is PinVerificationResult.LockedOut -> {
                        lockoutSeconds = result.secondsRemaining
                        errorMessage = "Too many failed attempts. Locked out for ${result.secondsRemaining}s."
                        enteredPin = ""
                    }
                    is PinVerificationResult.Invalid -> {
                        enteredPin = ""
                        errorMessage = if (result.attemptsRemaining > 0) {
                            "Incorrect PIN. ${result.attemptsRemaining} attempt(s) remaining."
                        } else {
                            "Incorrect PIN."
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (lockoutSeconds > 0) return
        errorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                        .border(1.5.dp, NovaTheme.colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "App Locked",
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "NovaFiles Locked",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Text(
                    text = if (lockoutSeconds > 0) {
                        "Try again in ${lockoutSeconds}s"
                    } else {
                        "Enter your 4-digit PIN to unlock"
                    },
                    style = NovaTheme.typography.body,
                    color = if (lockoutSeconds > 0) NovaTheme.colors.error else NovaTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                PinDotsIndicator(
                    enteredCount = enteredPin.length,
                    hasError = errorMessage != null
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = NovaTheme.colors.error,
                        style = NovaTheme.typography.captionMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Keypad Section
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinKeypad(
                    onDigitPress = { handleDigit(it) },
                    onBackspace = { handleBackspace() },
                    isLockedOut = lockoutSeconds > 0
                )
            }
        }
    }
}
