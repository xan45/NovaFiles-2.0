package com.novafiles.feature.vault

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novafiles.core.security.PinSecurityManager
import com.novafiles.core.security.PinVerificationResult
import com.novafiles.core.security.VaultCryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class VaultState {
    SETUP_PIN,
    LOCKED,
    LOCKED_OUT,
    UNLOCKED,
    DECOY_UNLOCKED
}

data class VaultItem(
    val file: File,
    val id: String = file.absolutePath,
    val originalName: String = file.name.removeSuffix(".nova"),
    val sizeBytes: Long = file.length(),
    val lastModified: Long = file.lastModified()
)

data class VaultUiState(
    val state: VaultState = VaultState.LOCKED,
    val enteredPin: String = "",
    val errorMessage: String? = null,
    val lockoutSeconds: Long = 0L,
    val vaultItems: List<VaultItem> = emptyList(),
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val showAddFileDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val selectedItemForRestore: VaultItem? = null,
    val selectedItemForDelete: VaultItem? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val pinSecurityManager = PinSecurityManager(application)
    private val primaryVaultDir = File(application.filesDir, "vault_primary").apply { mkdirs() }
    private val decoyVaultDir = File(application.filesDir, "vault_decoy").apply { mkdirs() }
    private val previewCacheDir = File(application.cacheDir, "vault_preview").apply { mkdirs() }

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var lockoutCountdownJob: Job? = null

    init {
        checkStatus()
    }

    fun checkStatus() {
        val remaining = pinSecurityManager.getRemainingLockoutSeconds()
        if (remaining > 0) {
            _uiState.update {
                it.copy(
                    state = VaultState.LOCKED_OUT,
                    lockoutSeconds = remaining,
                    errorMessage = "Too many failed attempts. Try again in ${remaining}s."
                )
            }
            startLockoutCountdown(remaining)
        } else if (!pinSecurityManager.hasPin) {
            _uiState.update { it.copy(state = VaultState.SETUP_PIN, enteredPin = "") }
        } else {
            _uiState.update {
                it.copy(
                    state = VaultState.LOCKED,
                    enteredPin = "",
                    isBiometricEnabled = pinSecurityManager.isBiometricEnabled
                )
            }
        }
    }

    fun onKeypadPress(digit: Char) {
        val current = _uiState.value.enteredPin
        if (current.length < 4) {
            val updated = current + digit
            _uiState.update { it.copy(enteredPin = updated, errorMessage = null) }
            if (updated.length == 4) {
                processEnteredPin(updated)
            }
        }
    }

    fun onKeypadBackspace() {
        val current = _uiState.value.enteredPin
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(enteredPin = current.dropLast(1), errorMessage = null) }
        }
    }

    private fun processEnteredPin(pin: String) {
        val currentState = _uiState.value.state
        viewModelScope.launch {
            if (currentState == VaultState.SETUP_PIN) {
                withContext(Dispatchers.Default) {
                    pinSecurityManager.setPin(pin.toCharArray())
                }
                _uiState.update { it.copy(state = VaultState.UNLOCKED, enteredPin = "") }
                loadVaultFiles(primaryVaultDir)
            } else {
                val result = withContext(Dispatchers.Default) {
                    pinSecurityManager.verifyPin(pin.toCharArray())
                }
                when (result) {
                    is PinVerificationResult.Success -> {
                        _uiState.update { it.copy(state = VaultState.UNLOCKED, enteredPin = "", errorMessage = null) }
                        loadVaultFiles(primaryVaultDir)
                    }
                    is PinVerificationResult.DecoySuccess -> {
                        _uiState.update { it.copy(state = VaultState.DECOY_UNLOCKED, enteredPin = "", errorMessage = null) }
                        loadVaultFiles(decoyVaultDir)
                    }
                    is PinVerificationResult.LockedOut -> {
                        _uiState.update {
                            it.copy(
                                state = VaultState.LOCKED_OUT,
                                enteredPin = "",
                                lockoutSeconds = result.secondsRemaining,
                                errorMessage = "Locked out for ${result.secondsRemaining} seconds."
                            )
                        }
                        startLockoutCountdown(result.secondsRemaining)
                    }
                    is PinVerificationResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                enteredPin = "",
                                errorMessage = "Incorrect PIN. ${result.attemptsRemaining} attempts remaining."
                            )
                        }
                    }
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(state = VaultState.UNLOCKED, errorMessage = null) }
        loadVaultFiles(primaryVaultDir)
    }

    private fun readEncryptedIndex(dir: File): MutableMap<String, String> {
        val indexFile = File(dir, "vault_index.enc")
        if (!indexFile.exists()) return mutableMapOf()
        val tempPlain = File(previewCacheDir, "idx_${dir.name}.tmp")
        return try {
            VaultCryptoEngine.decryptFile(indexFile, tempPlain)
            val map = mutableMapOf<String, String>()
            tempPlain.readLines().forEach { line ->
                val sep = line.indexOf('|')
                if (sep > 0) map[line.substring(0, sep)] = line.substring(sep + 1)
            }
            map
        } catch (_: Exception) {
            mutableMapOf()
        } finally {
            VaultCryptoEngine.wipeAndSecureDelete(tempPlain)
        }
    }

    private fun writeEncryptedIndex(dir: File, map: Map<String, String>) {
        val indexFile = File(dir, "vault_index.enc")
        val tempPlain = File(previewCacheDir, "idx_w_${dir.name}.tmp")
        try {
            tempPlain.writeText(map.entries.joinToString("\n") { "${it.key}|${it.value}" })
            VaultCryptoEngine.encryptFile(tempPlain, indexFile)
        } catch (_: Exception) {
        } finally {
            VaultCryptoEngine.wipeAndSecureDelete(tempPlain)
        }
    }

    fun loadVaultFiles(dir: File = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir) {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                val index = readEncryptedIndex(dir)
                dir.listFiles()?.filter { it.isFile && it.extension == "nova" }?.map { f ->
                    val resolvedName = index[f.name] ?: f.name.removeSuffix(".nova")
                    VaultItem(file = f, originalName = resolvedName)
                } ?: emptyList()
            }
            _uiState.update { it.copy(vaultItems = items.sortedByDescending { it.lastModified }) }
        }
    }

    fun encryptFileToVault(sourceFile: File) {
        viewModelScope.launch {
            val activeDir = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir
            val obfuscatedName = "${UUID.randomUUID()}.nova"
            val vaultFile = File(activeDir, obfuscatedName)
            try {
                withContext(Dispatchers.IO) {
                    VaultCryptoEngine.encryptFile(sourceFile, vaultFile)
                    val index = readEncryptedIndex(activeDir)
                    index[obfuscatedName] = sourceFile.name
                    writeEncryptedIndex(activeDir, index)
                }
                loadVaultFiles(activeDir)
                _uiState.update { it.copy(showAddFileDialog = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Encryption failed: ${e.message}") }
            }
        }
    }

    fun previewVaultItem(vaultItem: VaultItem, onReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val tempOut = File(previewCacheDir, vaultItem.originalName)
                withContext(Dispatchers.IO) {
                    VaultCryptoEngine.decryptFile(vaultItem.file, tempOut)
                }
                onReady(tempOut)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Preview failed: ${e.message}") }
            }
        }
    }

    fun restoreFile(vaultItem: VaultItem, destinationDir: File) {
        viewModelScope.launch {
            val activeDir = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir
            val destFile = File(destinationDir, vaultItem.originalName)
            try {
                withContext(Dispatchers.IO) {
                    VaultCryptoEngine.decryptFile(vaultItem.file, destFile)
                    VaultCryptoEngine.wipeAndSecureDelete(vaultItem.file)
                    val index = readEncryptedIndex(activeDir)
                    if (index.remove(vaultItem.file.name) != null) {
                        writeEncryptedIndex(activeDir, index)
                    }
                }
                loadVaultFiles()
                _uiState.update { it.copy(selectedItemForRestore = null, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Restore failed: ${e.message}") }
            }
        }
    }

    fun deleteVaultFile(vaultItem: VaultItem) {
        viewModelScope.launch {
            val activeDir = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir
            withContext(Dispatchers.IO) {
                VaultCryptoEngine.wipeAndSecureDelete(vaultItem.file)
                val index = readEncryptedIndex(activeDir)
                if (index.remove(vaultItem.file.name) != null) {
                    writeEncryptedIndex(activeDir, index)
                }
            }
            loadVaultFiles()
            _uiState.update { it.copy(selectedItemForDelete = null) }
        }
    }

    private fun wipePreviewCache() {
        previewCacheDir.listFiles()?.forEach { VaultCryptoEngine.wipeAndSecureDelete(it) }
    }

    fun lockVault() {
        viewModelScope.launch(Dispatchers.IO) { wipePreviewCache() }
        _uiState.update {
            it.copy(
                state = VaultState.LOCKED,
                enteredPin = "",
                vaultItems = emptyList()
            )
        }
    }

    fun setDecoyPin(pin: String) {
        viewModelScope.launch(Dispatchers.Default) {
            pinSecurityManager.setDecoyPin(pin.toCharArray())
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        pinSecurityManager.isBiometricEnabled = enabled
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun showAddFile(show: Boolean) = _uiState.update { it.copy(showAddFileDialog = show) }
    fun showSettings(show: Boolean) = _uiState.update { it.copy(showSettingsSheet = show) }
    fun setRestoreTarget(item: VaultItem?) = _uiState.update { it.copy(selectedItemForRestore = item) }
    fun setDeleteTarget(item: VaultItem?) = _uiState.update { it.copy(selectedItemForDelete = item) }

    private fun startLockoutCountdown(initialSeconds: Long) {
        lockoutCountdownJob?.cancel()
        lockoutCountdownJob = viewModelScope.launch {
            var remaining = initialSeconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update {
                    it.copy(
                        lockoutSeconds = remaining,
                        errorMessage = if (remaining > 0) "Locked out. Try again in ${remaining}s." else null
                    )
                }
            }
            _uiState.update { it.copy(state = VaultState.LOCKED, lockoutSeconds = 0, errorMessage = null) }
        }
    }

    override fun onCleared() {
        lockoutCountdownJob?.cancel()
        wipePreviewCache()
        super.onCleared()
    }
}
