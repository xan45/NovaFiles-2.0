@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.designsystem.theme.ColorBrightYellow
import com.novafiles.core.designsystem.theme.ColorCoralRed
import com.novafiles.core.designsystem.theme.ColorMintGreen
import com.novafiles.core.designsystem.theme.ColorNeonCyan
import com.novafiles.core.designsystem.theme.ColorVioletSecondary
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import com.novafiles.core.security.PinSecurityManager
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.PinSetupDialog

@Composable
fun SettingsScreen(
    onNavigateToFtp: () -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val pinSecurityManager = remember { PinSecurityManager(context) }
    var hasPinState by remember { mutableStateOf(pinSecurityManager.hasPin) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showLicensesSheet by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = NovaTheme.typography.display,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Section 1: Appearance & Personalization
                item {
                    SettingsSectionTitle("Appearance & Theme")
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Base Theme Mode",
                                style = NovaTheme.typography.body,
                                color = NovaTheme.colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            // Theme Mode 3-Pill Selector (White, Black, Custom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val isSelected = settings.themeConfig.mode == mode
                                    NovaButton(
                                        text = when (mode) {
                                            ThemeMode.WHITE -> "White"
                                            ThemeMode.BLACK -> "Black"
                                            ThemeMode.CUSTOM -> "Custom"
                                        },
                                        onClick = {
                                            val newConfig = settings.themeConfig.copy(mode = mode)
                                            viewModel.setThemeMode(mode)
                                            onThemeConfigChange(newConfig)
                                        },
                                        variant = if (isSelected) NovaButtonVariant.PRIMARY else NovaButtonVariant.SECONDARY,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // Live Preview Card
                            ThemePreviewCard(themeConfig = settings.themeConfig)

                            // Custom Theme Controls (Expanded only when CUSTOM is active)
                            if (settings.themeConfig.mode == ThemeMode.CUSTOM) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "Custom Theme Palette",
                                        style = NovaTheme.typography.body,
                                        color = NovaTheme.colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    CustomColorField(
                                        label = "Main Background",
                                        currentColor = settings.themeConfig.customBackground,
                                        presets = listOf(
                                            0xFF000000L, 0xFF0B1226L, 0xFF121212L, 0xFF0F172AL,
                                            0xFF18181BL, 0xFF1A102FL, 0xFFF8FAFCL, 0xFFFFFFFFL
                                        ),
                                        onColorChanged = { newColor ->
                                            val newConfig = settings.themeConfig.copy(customBackground = newColor)
                                            viewModel.setThemeConfig(newConfig)
                                            onThemeConfigChange(newConfig)
                                        }
                                    )

                                    CustomColorField(
                                        label = "Secondary Surface",
                                        currentColor = settings.themeConfig.customSurface,
                                        presets = listOf(
                                            0xFF0A0A0AL, 0xFF111B3AL, 0xFF1E1E1EL, 0xFF1E293BL,
                                            0xFF27272AL, 0xFF261947L, 0xFFF1F5F9L
                                        ),
                                        onColorChanged = { newColor ->
                                            val newConfig = settings.themeConfig.copy(customSurface = newColor)
                                            viewModel.setThemeConfig(newConfig)
                                            onThemeConfigChange(newConfig)
                                        }
                                    )

                                    CustomColorField(
                                        label = "Card / Container",
                                        currentColor = settings.themeConfig.customCard,
                                        presets = listOf(
                                            0xFF141414L, 0xFF1A2547L, 0xFF2A2A2AL, 0xFF334155L,
                                            0xFF3F3F46L, 0xFF352562L, 0xFFE2E8F0L
                                        ),
                                        onColorChanged = { newColor ->
                                            val newConfig = settings.themeConfig.copy(customCard = newColor)
                                            viewModel.setThemeConfig(newConfig)
                                            onThemeConfigChange(newConfig)
                                        }
                                    )

                                    NovaButton(
                                        text = "Reset to Default Custom Colors",
                                        icon = Icons.Rounded.Refresh,
                                        onClick = {
                                            viewModel.resetCustomColors()
                                            val resetConfig = settings.themeConfig.copy(
                                                customBackground = 0xFF121212L,
                                                customSurface = 0xFF1E1E1EL,
                                                customCard = 0xFF2A2A2AL
                                            )
                                            onThemeConfigChange(resetConfig)
                                        },
                                        variant = NovaButtonVariant.SECONDARY,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Accent Color (6 Curated Options)",
                                style = NovaTheme.typography.body,
                                color = NovaTheme.colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            // Accent Swatches
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AccentColor.entries.forEach { accent ->
                                    val isSelected = settings.themeConfig.accent == accent
                                    val swatchColor = getAccentSwatchColor(accent)

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val newConfig = settings.themeConfig.copy(accent = accent)
                                                viewModel.setAccentColor(accent)
                                                onThemeConfigChange(newConfig)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section 2: File Browser Behavior
                item {
                    SettingsSectionTitle("File Management")
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Confirm Deletion Switch
                            SettingsToggleRow(
                                title = "Confirm Deletions",
                                subtitle = "Ask for confirmation before permanently deleting files",
                                icon = Icons.Rounded.Delete,
                                checked = settings.confirmFileDeletion,
                                onCheckedChange = { viewModel.setConfirmFileDeletion(it) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section 3: Tools & Network Services
                item {
                    SettingsSectionTitle("Network & Storage Tools")
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SettingsActionRow(
                                title = "FTP File Sharing Server",
                                subtitle = "Share and transfer files over local Wi-Fi",
                                icon = Icons.Rounded.Wifi,
                                onClick = onNavigateToFtp
                            )

                            SettingsActionRow(
                                title = "Storage Analyzer",
                                subtitle = "Visual usage breakdown and large file audit",
                                icon = Icons.Rounded.PieChart,
                                onClick = onNavigateToAnalyzer
                            )

                            SettingsActionRow(
                                title = "Storage Junk Cleaner",
                                subtitle = "Clean cache, temp logs, and obsolete APKs",
                                icon = Icons.Rounded.CleaningServices,
                                onClick = onNavigateToCleaner
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section 4: Security & Privacy
                item {
                    SettingsSectionTitle("Security & Privacy")
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SettingsToggleRow(
                                title = "App Lock on Launch",
                                subtitle = "Require PIN upon opening NovaFiles",
                                icon = Icons.Rounded.Lock,
                                checked = settings.isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (!pinSecurityManager.hasPin) {
                                            showSetPinDialog = true
                                        } else {
                                            viewModel.setAppLock(true)
                                        }
                                    } else {
                                        viewModel.setAppLock(false)
                                    }
                                }
                            )

                            if (hasPinState) {
                                SettingsActionRow(
                                    title = "Change App Lock PIN",
                                    subtitle = "Update your existing 4-digit PIN",
                                    icon = Icons.Rounded.Security,
                                    onClick = { showSetPinDialog = true }
                                )

                                SettingsActionRow(
                                    title = "Remove PIN",
                                    subtitle = "Delete stored PIN and disable App Lock",
                                    icon = Icons.Rounded.Delete,
                                    onClick = {
                                        pinSecurityManager.removePin()
                                        hasPinState = false
                                        viewModel.setAppLock(false)
                                    }
                                )
                            } else {
                                SettingsActionRow(
                                    title = "Set App Lock PIN",
                                    subtitle = "Configure a secure 4-digit PIN",
                                    icon = Icons.Rounded.Security,
                                    onClick = { showSetPinDialog = true }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Section 5: About & System
                item {
                    SettingsSectionTitle("About NovaFiles")
                    Spacer(modifier = Modifier.height(10.dp))

                    NovaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NovaTheme.colors.surfaceElevated
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SettingsActionRow(
                                title = "About NovaFiles",
                                subtitle = "Advanced Storage & File Management Suite • v1.0.0-alpha1",
                                icon = Icons.Rounded.Info,
                                onClick = onNavigateToAbout
                            )

                            SettingsActionRow(
                                title = "Open Source Licenses",
                                subtitle = "Third-party software libraries and licenses",
                                icon = Icons.Rounded.Security,
                                onClick = { showLicensesSheet = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Open Source Licenses Sheet
        if (showLicensesSheet) {
            LicensesSheet(onDismiss = { showLicensesSheet = false })
        }

        if (showSetPinDialog) {
            PinSetupDialog(
                onDismissRequest = { showSetPinDialog = false },
                onPinConfirmed = { pin ->
                    pinSecurityManager.setPin(pin)
                    hasPinState = true
                    viewModel.setAppLock(true)
                    showSetPinDialog = false
                },
                title = if (hasPinState) "Change App Lock PIN" else "Set App Lock PIN"
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = NovaTheme.typography.title,
        color = NovaTheme.colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NovaTheme.colors.primary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = NovaTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun getAccentSwatchColor(accent: AccentColor): Color {
    return when (accent) {
        AccentColor.INDIGO -> Color(0xFF5B6EF5)
        AccentColor.EMERALD -> Color(0xFF10B981)
        AccentColor.CORAL -> Color(0xFFFF6B6B)
        AccentColor.CYAN -> Color(0xFF2DD4BF)
        AccentColor.AMBER -> Color(0xFFFFD166)
        AccentColor.PINK -> Color(0xFFF472B6)
    }
}

@Composable
private fun LicensesSheet(onDismiss: () -> Unit) {
    val licenses = listOf(
        "AndroidX Jetpack & Jetpack Compose (Apache 2.0)",
        "Kotlin Coroutines & Serialization (Apache 2.0)",
        "AndroidX Media3 ExoPlayer (Apache 2.0)",
        "Coil Image Loading (Apache 2.0)",
        "Apache Commons Compress (Apache 2.0)"
    )

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Open Source Licenses",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            licenses.forEach { lib ->
                Text(
                    text = lib,
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ThemePreviewCard(
    themeConfig: ThemeConfig,
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = NovaTheme.colors.background,
        borderColor = NovaTheme.colors.cardBorder
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NovaTheme.colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Theme Preview",
                        style = NovaTheme.typography.subtitle,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = when (themeConfig.mode) {
                        ThemeMode.WHITE -> "White"
                        ThemeMode.BLACK -> "Black AMOLED"
                        ThemeMode.CUSTOM -> "Custom"
                    },
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovaTheme.colors.surfaceElevated)
                    .border(1.dp, NovaTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NovaFiles 2.0",
                            style = NovaTheme.typography.body,
                            color = NovaTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Theme & contrast preview",
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NovaTheme.colors.primary)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = themeConfig.accent.name,
                            style = NovaTheme.typography.caption,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomColorField(
    label: String,
    currentColor: Long,
    presets: List<Long>,
    onColorChanged: (Long) -> Unit
) {
    var hexText by remember(currentColor) {
        mutableStateOf(String.format(java.util.Locale.US, "%06X", currentColor and 0x00FFFFFFL))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                        .border(1.dp, NovaTheme.colors.cardBorder, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "#$hexText",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Preset color swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = (currentColor and 0x00FFFFFFL) == (preset and 0x00FFFFFFL)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(preset))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) NovaTheme.colors.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onColorChanged(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NovaTheme.colors.primary)
                        )
                    }
                }
            }
        }

        // Hex Code input field
        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                val filtered = input.take(6).uppercase()
                hexText = filtered
                if (filtered.length == 6) {
                    val parsed = filtered.toLongOrNull(16)
                    if (parsed != null) {
                        onColorChanged(0xFF000000L or parsed)
                    }
                }
            },
            prefix = { Text("#", color = NovaTheme.colors.textSecondary) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = NovaTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NovaTheme.colors.primary,
                unfocusedBorderColor = NovaTheme.colors.cardBorder,
                focusedContainerColor = NovaTheme.colors.surface,
                unfocusedContainerColor = NovaTheme.colors.surface
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text("HEX code", color = NovaTheme.colors.textSecondary, fontSize = 13.sp) }
        )
    }
}
