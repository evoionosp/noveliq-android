package org.evoionosp.noveliq.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.usecase.LoginUseCase
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.usecase.ServerPingUseCase
import org.evoionosp.noveliq.domain.server.usecase.ServerHealthCheckUseCase
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val serverPingUseCase: ServerPingUseCase,
    private val serverHealthCheckUseCase: ServerHealthCheckUseCase,
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(uiMessageResId = null) }
    }

    fun checkLoginSetup() {
        val baseUrl = _uiState.value.baseUrl.trim()
        if (baseUrl.isBlank()) {
            setLoginSetupError(R.string.error_server_url_required)
            return
        }
        if (!baseUrl.lowercase(Locale.US).startsWith("http://") &&
            !baseUrl.lowercase(Locale.US).startsWith("https://")
        ) {
            setLoginSetupError(R.string.error_server_url_scheme)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, uiMessageResId = null) }

            when (val checkResult = serverPingUseCase(baseUrl)) {
                is ServerCheckResult.Failure -> {
                    setLoginSetupError(toLoginCheckErrorRes(checkResult.error))
                }
                is ServerCheckResult.Success -> {
                    when (val healthResult = serverHealthCheckUseCase(baseUrl)) {
                        is ServerCheckResult.Failure -> setLoginSetupError(toLoginCheckErrorRes(healthResult.error))
                        is ServerCheckResult.Success -> {
                            if (healthResult.data) {
                                _uiState.update {
                                    it.copy(
                                        isChecking = false,
                                        showLoginFields = true,
                                        serverStatus = checkResult.data
                                    )
                                }
                            } else {
                                setLoginSetupError(R.string.error_healthcheck_failed)
                            }
                        }
                    }
                }
            }
        }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank()) {
            setLoginError(R.string.error_username_required)
            return
        }
        if (currentState.password.isBlank()) {
            setLoginError(R.string.error_password_required)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, uiMessageResId = null) }
            when (val result = loginUseCase(
                baseUrl = currentState.baseUrl.trim(),
                username = currentState.username.trim(),
                password = currentState.password
            )) {
                is LoginResult.Success -> {
                    _uiState.update {
                        it.copy(isLoggingIn = false, uiMessageResId = R.string.login_success)
                    }
                }
                is LoginResult.Failure -> {
                    setLoginError(toAuthErrorRes(result.error))
                }
            }
        }
    }

    private fun setLoginSetupError(messageResId: Int) {
        _uiState.update {
            it.copy(
                isChecking = false,
                isLoggingIn = false,
                showLoginFields = false,
                uiMessageResId = messageResId
            )
        }
    }

    private fun setLoginError(messageResId: Int) {
        _uiState.update {
            it.copy(
                isChecking = false,
                isLoggingIn = false,
                uiMessageResId = messageResId
            )
        }
    }

    private fun toLoginCheckErrorRes(error: ServerError): Int {
        return when (error) {
            ServerError.INVALID_BASE_URL -> R.string.error_invalid_base_url
            ServerError.NETWORK -> R.string.error_network
            ServerError.HTTP -> R.string.error_http
            ServerError.PING_FAILED -> R.string.error_ping_failed
            ServerError.HEALTHCHECK_FAILED -> R.string.error_healthcheck_failed
            ServerError.HEALTHCHECK_UNEXPECTED -> R.string.error_healthcheck_unexpected
            ServerError.UNKNOWN -> R.string.error_unknown
        }
    }

    private fun toAuthErrorRes(error: AuthError): Int {
        return when (error) {
            AuthError.INVALID_BASE_URL -> R.string.error_invalid_base_url
            AuthError.NETWORK -> R.string.error_network
            AuthError.HTTP -> R.string.error_login_failed
            AuthError.UNEXPECTED -> R.string.error_unknown
        }
    }
}
