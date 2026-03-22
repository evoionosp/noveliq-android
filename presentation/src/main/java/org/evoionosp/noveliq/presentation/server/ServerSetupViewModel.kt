package org.evoionosp.noveliq.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.data.server.ServerRepositoryProvider
import org.evoionosp.noveliq.domain.server.model.ServerResult
import org.evoionosp.noveliq.domain.server.usecase.CheckServerUseCase
import org.evoionosp.noveliq.domain.server.usecase.HealthCheckUseCase

class ServerSetupViewModel(
    private val checkServerUseCase: CheckServerUseCase,
    private val healthCheckUseCase: HealthCheckUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServerSetupUiState())
    val uiState: StateFlow<ServerSetupUiState> = _uiState.asStateFlow()

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun checkServer() {
        val baseUrl = _uiState.value.baseUrl.trim()
        if (baseUrl.isBlank()) {
            setError("Please enter a server URL.")
            return
        }
        if (!baseUrl.lowercase(Locale.US).startsWith("http://") &&
            !baseUrl.lowercase(Locale.US).startsWith("https://")
        ) {
            setError("Server URL must start with http:// or https://")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }

            when (val checkResult = checkServerUseCase(baseUrl)) {
                is ServerResult.Failure -> {
                    setError(checkResult.message)
                }
                is ServerResult.Success -> {
                    when (val healthResult = healthCheckUseCase(baseUrl)) {
                        is ServerResult.Failure -> setError(healthResult.message)
                        is ServerResult.Success -> {
                            if (healthResult.data) {
                                _uiState.update {
                                    it.copy(
                                        isChecking = false,
                                        showLoginFields = true,
                                        serverStatus = checkResult.data
                                    )
                                }
                            } else {
                                setError("Healthcheck failed.")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                isChecking = false,
                showLoginFields = false,
                errorMessage = message
            )
        }
    }
}

class ServerSetupViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = ServerRepositoryProvider.create()
        val checkServerUseCase = CheckServerUseCase(repository)
        val healthCheckUseCase = HealthCheckUseCase(repository)
        return ServerSetupViewModel(
            checkServerUseCase = checkServerUseCase,
            healthCheckUseCase = healthCheckUseCase
        ) as T
    }
}
