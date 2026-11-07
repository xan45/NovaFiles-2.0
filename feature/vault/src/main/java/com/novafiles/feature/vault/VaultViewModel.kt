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
        if (currentState == VaultState.SETUP_PIN) {
            pinSecurityManager.setPin(pin.toCharArray())
            _uiState.update { it.copy(state = VaultState.UNLOCKED, enteredPin = "") }
            loadVaultFiles(primaryVaultDir)
        } else {
            val result = pinSecurityManager.verifyPin(pin.toCharArray())
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

    fun onBiometricSuccess() {
        _uiState.update { it.copy(state = VaultState.UNLOCKED, errorMessage = null) }
        loadVaultFiles(primaryVaultDir)
    }

    fun loadVaultFiles(dir: File = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir) {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                dir.listFiles()?.filter { it.isFile && it.extension == "nova" }?.map {
                    VaultItem(file = it)
                } ?: emptyList()
            }
            _uiState.update { it.copy(vaultItems = items.sortedByDescending { it.lastModified }) }
        }
    }

    fun encryptFileToVault(sourceFile: File) {
        viewModelScope.launch {
            val activeDir = if (_uiState.value.state == VaultState.DECOY_UNLOCKED) decoyVaultDir else primaryVaultDir
            val vaultFile = File(activeDir, "${sourceFile.name}.nova")
            withContext(Dispatchers.IO) {
                VaultCryptoEngine.encryptFile(sourceFile, vaultFile)
            }
            loadVaultFiles(activeDir)
            _uiState.update { it.copy(showAddFileDialog = false) }
        }
    }

    fun restoreFile(vaultItem: VaultItem, destinationDir: File) {
        viewModelScope.launch {
            val destFile = File(destinationDir, vaultItem.originalName)
            withContext(Dispatchers.IO) {
                VaultCryptoEngine.decryptFile(vaultItem.file, destFile)
                VaultCryptoEngine.wipeAndSecureDelete(vaultItem.file)
            }
            loadVaultFiles()
            _uiState.update { it.copy(selectedItemForRestore = null) }
        }
    }

    fun deleteVaultFile(vaultItem: VaultItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                VaultCryptoEngine.wipeAndSecureDelete(vaultItem.file)
            }
            loadVaultFiles()
            _uiState.update { it.copy(selectedItemForDelete = null) }
        }
    }

    fun lockVault() {
        _uiState.update {
            it.copy(
                state = VaultState.LOCKED,
                enteredPin = "",
                vaultItems = emptyList()
            )
        }
    }

    fun setDecoyPin(pin: String) {
        pinSecurityManager.setDecoyPin(pin.toCharArray())
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
        super.onCleared()
    }
}
