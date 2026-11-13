@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.ftp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.designsystem.theme.ColorBrightYellow
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorMintGreen
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard

@Composable
fun FtpServerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FtpViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRunning = uiState.serverState.isRunning
    val ftpUrl = "ftp://${uiState.serverState.ipAddress}:${uiState.config.port}"
    val httpUrl = "http://${uiState.serverState.ipAddress}:${uiState.config.port + 1}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaTheme.colors.textPrimary
                    )
                }

                Text(
                    text = "FTP Network Server",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                IconButton(onClick = { viewModel.checkWifiStatus() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Wi-Fi",
                        tint = NovaTheme.colors.textPrimary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Wi-Fi Status Banner
                item {
                    WifiStatusBanner(isWifiConnected = uiState.isWifiConnected)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Power & Status Card
                item {
                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = NovaTheme.shapes.extraLarge,
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Glowing Power Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isRunning) ColorMintGreen.copy(alpha = 0.2f)
                                        else NovaTheme.colors.surface
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isRunning) ColorMintGreen else NovaTheme.colors.cardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.toggleServer() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PowerSettingsNew,
                                    contentDescription = "Toggle Server",
                                    tint = if (isRunning) ColorMintGreen else NovaTheme.colors.textSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isRunning) "Server Active" else "Server Stopped",
                                style = NovaTheme.typography.title,
                                color = if (isRunning) ColorMintGreen else NovaTheme.colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isRunning) "Access files via any browser or FTP client on your network" else "Tap below to start sharing files over local Wi-Fi",
                                style = NovaTheme.typography.captionMedium,
                                color = NovaTheme.colors.textSecondary
                            )

                            if (isRunning) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // URL Pill with Copy
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(NovaTheme.shapes.pill)
                                        .background(NovaTheme.colors.surface)
                                        .border(1.dp, NovaTheme.colors.cardBorder, NovaTheme.shapes.pill)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = ftpUrl,
                                            color = NovaTheme.colors.primary,
                                            style = NovaTheme.typography.body,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Browser: $httpUrl",
                                            color = NovaTheme.colors.textSecondary,
                                            style = NovaTheme.typography.caption,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.clickable {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("HTTP Share URL", httpUrl))
                                                Toast.makeText(context, "Browser Share URL copied", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("FTP URL", ftpUrl)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "FTP URL copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy URL",
                                            tint = NovaTheme.colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            NovaButton(
                                text = if (isRunning) "Stop Server" else "Start Server",
                                onClick = { viewModel.toggleServer() },
                                variant = if (isRunning) NovaButtonVariant.SECONDARY else NovaButtonVariant.GRADIENT,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Authentication Credentials Card
                item {
                    Text(
                        text = "Connection Credentials",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CredentialRow(
                                label = "Username",
                                value = if (uiState.config.isAnonymousAllowed) "anonymous (Any)" else uiState.config.username
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Password",
                                        style = NovaTheme.typography.caption,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.showPassword) uiState.config.password else "••••••••",
                                        style = NovaTheme.typography.body,
                                        color = NovaTheme.colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.togglePasswordVisibility() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = "Toggle Password",
                                            tint = NovaTheme.colors.textSecondary
                                        )
                                    }

                                    if (!isRunning) {
                                        IconButton(
                                            onClick = { viewModel.regeneratePassword() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Regenerate Password",
                                                tint = NovaTheme.colors.primary
                                            )
                                        }
                                    }
                                }
                            }

                            CredentialRow(
                                label = "Port",
                                value = uiState.config.port.toString()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Security & Sandbox Policies
                item {
                    Text(
                        text = "Security Policies",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Read-Only Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Read-Only Mode",
                                        style = NovaTheme.typography.body,
                                        color = NovaTheme.colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Prevents client file uploads, modifications, or deletions",
                                        style = NovaTheme.typography.caption,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }

                                Switch(
                                    checked = uiState.config.isReadOnly,
                                    onCheckedChange = { viewModel.toggleReadOnly(it) },
                                    enabled = !isRunning,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NovaTheme.colors.primary
                                    )
                                )
                            }

                            // Anonymous Access Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow Anonymous Access",
                                        style = NovaTheme.typography.body,
                                        color = NovaTheme.colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Allows any network client to connect without password",
                                        style = NovaTheme.typography.caption,
                                        color = NovaTheme.colors.textSecondary
                                    )
                                }

                                Switch(
                                    checked = uiState.config.isAnonymousAllowed,
                                    onCheckedChange = { viewModel.toggleAnonymous(it) },
                                    enabled = !isRunning,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NovaTheme.colors.primary
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Server Access Terminal Logs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity Log",
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Active Clients: ${uiState.serverState.activeClientsCount}",
                            style = NovaTheme.typography.caption,
                            color = if (uiState.serverState.activeClientsCount > 0) ColorMintGreen else NovaTheme.colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B18))
                            .border(1.dp, NovaTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (uiState.serverState.logs.isEmpty()) {
                            Text(
                                text = "No server activity yet.",
                                color = NovaTheme.colors.textSecondary.copy(alpha = 0.5f),
                                style = NovaTheme.typography.caption,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            LazyColumn(reverseLayout = true) {
                                items(uiState.serverState.logs.reversed()) { logEntry ->
                                    Text(
                                        text = logEntry,
                                        color = if (logEntry.contains("Error", true) || logEntry.contains("Failed", true)) ColorCoralRed
                                        else if (logEntry.contains("authenticated", true)) ColorMintGreen
                                        else NovaTheme.colors.textSecondary,
                                        style = NovaTheme.typography.caption,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun WifiStatusBanner(isWifiConnected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isWifiConnected) NovaTheme.colors.surfaceElevated
                else ColorCoralRed.copy(alpha = 0.15f)
            )
            .border(
                1.dp,
                if (isWifiConnected) NovaTheme.colors.cardBorder else ColorCoralRed.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isWifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
            contentDescription = null,
            tint = if (isWifiConnected) ColorMintGreen else ColorCoralRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = if (isWifiConnected) "Connected to Local Wi-Fi" else "Wi-Fi Disconnected",
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isWifiConnected) "Other devices on this network can transfer files" else "Connect to a Wi-Fi network or enable portable hotspot to share files",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = NovaTheme.typography.caption,
            color = NovaTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = NovaTheme.typography.body,
            color = NovaTheme.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
