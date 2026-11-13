@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.novafiles.feature.vault

import android.os.Environment
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.security.BiometricAuthManager
import com.novafiles.core.ui.components.NovaBottomSheet
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaConfirmDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Block screenshots & recents thumbnails while Vault is open
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto-lock Vault when app moves to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (uiState.state == VaultState.UNLOCKED || uiState.state == VaultState.DECOY_UNLOCKED) {
                    viewModel.lockVault()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaTheme.colors.background)
    ) {
        when (uiState.state) {
            VaultState.SETUP_PIN,
            VaultState.LOCKED,
            VaultState.LOCKED_OUT -> {
                VaultPinKeypadScreen(
                    uiState = uiState,
                    onDigitPress = { viewModel.onKeypadPress(it) },
                    onBackspace = { viewModel.onKeypadBackspace() },
                    onBiometricClick = {
                        (context as? FragmentActivity)?.let { activity ->
                            BiometricAuthManager.promptBiometric(
                                activity = activity,
                                onSuccess = { viewModel.onBiometricSuccess() },
                                onError = { /* fallback to pin */ }
                            )
                        }
                    },
                    onNavigateBack = onNavigateBack
                )
            }
            VaultState.UNLOCKED,
            VaultState.DECOY_UNLOCKED -> {
                VaultUnlockedScreen(
                    uiState = uiState,
                    onLock = { viewModel.lockVault() },
                    onAddFileClick = { viewModel.showAddFile(true) },
                    onRestoreItem = { viewModel.setRestoreTarget(it) },
                    onDeleteItem = { viewModel.setDeleteTarget(it) },
                    onOpenSettings = { viewModel.showSettings(true) },
                    onNavigateBack = onNavigateBack
                )
            }
        }

        // Add File Bottom Sheet Picker
        if (uiState.showAddFileDialog) {
            AddFileToVaultSheet(
                onSelectFile = { file ->
                    viewModel.encryptFileToVault(file)
                },
                onDismiss = { viewModel.showAddFile(false) }
            )
        }

        // Restore Target Folder Sheet
        if (uiState.selectedItemForRestore != null) {
            RestoreFolderPickerSheet(
                item = uiState.selectedItemForRestore!!,
                onSelectFolder = { folder ->
                    viewModel.restoreFile(uiState.selectedItemForRestore!!, folder)
                },
                onDismiss = { viewModel.setRestoreTarget(null) }
            )
        }

        // Delete Confirm Dialog (Zero-wipe warning)
        if (uiState.selectedItemForDelete != null) {
            NovaConfirmDialog(
                title = "Permanently Destroy File?",
                message = "\"${uiState.selectedItemForDelete!!.originalName}\" will be securely overwritten with zeros and permanently deleted from storage. This cannot be undone.",
                confirmText = "Destroy",
                isDestructive = true,
                onConfirm = { viewModel.deleteVaultFile(uiState.selectedItemForDelete!!) },
                onDismiss = { viewModel.setDeleteTarget(null) }
            )
        }

        // Settings Sheet
        if (uiState.showSettingsSheet) {
            VaultSettingsSheet(
                isBiometricEnabled = uiState.isBiometricEnabled,
                onToggleBiometric = { viewModel.toggleBiometric(it) },
                onSetDecoyPin = { viewModel.setDecoyPin(it) },
                onDismiss = { viewModel.showSettings(false) }
            )
        }
    }
}

@Composable
private fun VaultPinKeypadScreen(
    uiState: VaultUiState,
    onDigitPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = NovaTheme.colors.textPrimary
                )
            }
        }

        // Header and Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f))
                    .border(1.5.dp, NovaTheme.colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (uiState.state == VaultState.SETUP_PIN) "Create Vault PIN" else "NovaFiles Vault",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Text(
                text = if (uiState.state == VaultState.SETUP_PIN) "Choose a 4-digit PIN to encrypt your files" else "Enter your 4-digit PIN to access encrypted files",
                style = NovaTheme.typography.body,
                color = NovaTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Dot Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < uiState.enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) NovaTheme.colors.primary else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (isFilled) NovaTheme.colors.primary else NovaTheme.colors.cardBorder,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error Message Banner
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = NovaTheme.colors.error,
                    style = NovaTheme.typography.captionMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Keypad Grid (3 columns)
        val isLockedOut = uiState.state == VaultState.LOCKED_OUT
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        KeypadButton(
                            text = digit.toString(),
                            enabled = !isLockedOut,
                            onClick = { onDigitPress(digit) }
                        )
                    }
                }
            }

            // Bottom row: Biometric, 0, Backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Biometric Button
                if (uiState.isBiometricEnabled) {
                    IconButton(
                        onClick = onBiometricClick,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Unlock with Biometrics",
                            tint = NovaTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(68.dp))
                }

                KeypadButton(
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
                        tint = NovaTheme.colors.textSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(NovaTheme.colors.surfaceElevated)
            .border(1.dp, NovaTheme.colors.cardBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) NovaTheme.colors.textPrimary else NovaTheme.colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun VaultUnlockedScreen(
    uiState: VaultUiState,
    onLock: () -> Unit,
    onAddFileClick: () -> Unit,
    onRestoreItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isDecoy = uiState.state == VaultState.DECOY_UNLOCKED

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDecoy) "Vault (Safe)" else "Secure Vault",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onLock) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock Vault",
                            tint = NovaTheme.colors.warning
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Vault Settings",
                            tint = NovaTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Security Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.surfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = NovaTheme.colors.success,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AES-256-GCM Hardware-Backed Encryption • NOVAV1",
                    color = NovaTheme.colors.success,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File List or Empty State
            if (uiState.vaultItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = NovaTheme.colors.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Your Vault is Empty",
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary
                        )
                        Text(
                            text = "Tap the + button below to encrypt and secure confidential files.",
                            style = NovaTheme.typography.body,
                            color = NovaTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    items(uiState.vaultItems) { item ->
                        VaultItemRow(
                            item = item,
                            onRestore = { onRestoreItem(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Floating Action Button to add file
        FloatingActionButton(
            onClick = onAddFileClick,
            containerColor = NovaTheme.colors.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Encrypt File")
        }
    }
}

@Composable
private fun VaultItemRow(
    item: VaultItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NovaTheme.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NovaTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = NovaTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalName,
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FileFormatters.formatBytes(item.sizeBytes)} • Encrypted ${SimpleDateFormat("MMM dd", Locale.US).format(Date(item.lastModified))}",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = "Restore to Storage",
                    tint = NovaTheme.colors.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Destroy",
                    tint = NovaTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun AddFileToVaultSheet(
    onSelectFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleFiles = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    ).flatMap { it?.listFiles()?.toList() ?: emptyList() }
        .filter { it.isFile && !it.name.endsWith(".nova") }
        .take(15)

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Select File to Encrypt",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Text(
                text = "The file will be encrypted into your secure vault and safely wiped from public storage.",
                style = NovaTheme.typography.caption,
                color = NovaTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (sampleFiles.isEmpty()) {
                Text(
                    text = "No files found in Documents, Downloads, or Pictures.",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(sampleFiles) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectFile(file) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = NovaTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = NovaTheme.typography.body,
                                    color = NovaTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FileFormatters.formatBytes(file.length()),
                                    style = NovaTheme.typography.caption,
                                    color = NovaTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreFolderPickerSheet(
    item: VaultItem,
    onSelectFolder: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val commonFolders = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    ).filterNotNull()

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Restore \"${item.originalName}\" to",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            commonFolders.forEach { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NovaTheme.colors.surfaceElevated)
                        .border(1.dp, NovaTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelectFolder(folder) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = NovaTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = folder.name,
                            style = NovaTheme.typography.title,
                            color = NovaTheme.colors.textPrimary
                        )
                        Text(
                            text = folder.absolutePath,
                            style = NovaTheme.typography.caption,
                            color = NovaTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultSettingsSheet(
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onSetDecoyPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var decoyPinInput by remember { mutableStateOf("") }
    var decoySaved by remember { mutableStateOf(false) }

    NovaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Vault Security Settings",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )

            // Biometric Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric Unlock",
                        style = NovaTheme.typography.body,
                        color = NovaTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Unlock vault using fingerprint or face authentication",
                        style = NovaTheme.typography.caption,
                        color = NovaTheme.colors.textSecondary
                    )
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = onToggleBiometric,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NovaTheme.colors.primary
                    )
                )
            }

            // Decoy PIN Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Decoy Vault PIN (Duress Protection)",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "If forced to open the vault, entering this decoy PIN opens an isolated fake vault with innocuous files, protecting your real confidential data.",
                    style = NovaTheme.typography.caption,
                    color = NovaTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovaButton(
                        text = if (decoySaved) "Decoy PIN Set!" else "Set Decoy PIN (9999)",
                        onClick = {
                            onSetDecoyPin("9999")
                            decoySaved = true
                        },
                        variant = if (decoySaved) NovaButtonVariant.SECONDARY else NovaButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
