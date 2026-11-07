package com.novafiles.feature.ftp

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FtpUiState(
    val serverState: FtpServerState = FtpServerState(),
    val config: FtpServerConfig = FtpServerConfig(),
    val isWifiConnected: Boolean = true,
    val showPassword: Boolean = false
)

class FtpViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FtpUiState())
    val uiState: StateFlow<FtpUiState> = _uiState.asStateFlow()

    init {
        checkWifiStatus()
        viewModelScope.launch {
            FtpServerEngine.serverState.collect { state ->
                _uiState.update { it.copy(serverState = state) }
            }
        }
    }

    fun checkWifiStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val isWifi = cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        _uiState.update { it.copy(isWifiConnected = isWifi) }
    }

    fun toggleServer() {
        val current = _uiState.value
        if (current.serverState.isRunning) {
            FtpForegroundService.stop(getApplication())
        } else {
            FtpForegroundService.start(getApplication(), current.config)
        }
    }

    fun setPort(port: Int) {
        if (!_uiState.value.serverState.isRunning) {
            _uiState.update { it.copy(config = it.config.copy(port = port.coerceIn(1024, 65535))) }
        }
    }

    fun setUsername(username: String) {
        if (!_uiState.value.serverState.isRunning && username.isNotBlank()) {
            _uiState.update { it.copy(config = it.config.copy(username = username.trim())) }
        }
    }

    fun setPassword(password: String) {
        if (!_uiState.value.serverState.isRunning && password.isNotBlank()) {
            _uiState.update { it.copy(config = it.config.copy(password = password.trim())) }
        }
    }

    fun toggleReadOnly(isReadOnly: Boolean) {
        if (!_uiState.value.serverState.isRunning) {
            _uiState.update { it.copy(config = it.config.copy(isReadOnly = isReadOnly)) }
        }
    }

    fun toggleAnonymous(isAnonymous: Boolean) {
        if (!_uiState.value.serverState.isRunning) {
            _uiState.update { it.copy(config = it.config.copy(isAnonymousAllowed = isAnonymous)) }
        }
    }

    fun regeneratePassword() {
        if (!_uiState.value.serverState.isRunning) {
            _uiState.update { it.copy(config = it.config.copy(password = FtpServerConfig.generateSecurePassword())) }
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }
}
