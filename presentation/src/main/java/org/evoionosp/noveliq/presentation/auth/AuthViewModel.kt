package org.evoionosp.noveliq.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.usecase.ObserveLastServerUrlUseCase
import org.evoionosp.noveliq.domain.session.usecase.SaveSessionUseCase
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.usecase.LoginUseCase
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.usecase.ServerPingUseCase
import org.evoionosp.noveliq.domain.server.usecase.ServerHealthCheckUseCase
import org.evoionosp.noveliq.presentation.R

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val serverPingUseCase: ServerPingUseCase,
    private val serverHealthCheckUseCase: ServerHealthCheckUseCase,
    private val loginUseCase: LoginUseCase,
    private val saveSessionUseCase: SaveSessionUseCase,
    private val observeLastServerUrlUseCase: ObserveLastServerUrlUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    init {
        // Offer the server they used last. Someone arriving here has either logged out or been
        // pushed back by an expired session, and either way they are almost certainly signing
        // back in to the same server.
        viewModelScope.launch {
            val lastServerUrl = observeLastServerUrlUseCase().first()
            if (lastServerUrl.isNullOrBlank()) return@launch

            val (protocol, host) = splitProtocol(lastServerUrl)
            _uiState.update { state ->
                // Never overwrite something already typed: the prefill loses the race only if the
                // user was faster, and their input wins.
                if (state.baseUrl.isNotBlank()) state
                else state.copy(protocol = protocol, baseUrl = host)
            }
        }
    }

    fun onProtocolChange(value: String) {
        _uiState.update { it.copy(protocol = value) }
    }
    fun onBaseUrlChange(value: String) {
        val trimmedValue = value.trim()
        val (newProtocol, newBaseUrl) = when {
            trimmedValue.startsWith("https://", ignoreCase = true) ||
                trimmedValue.startsWith("http://", ignoreCase = true) -> splitProtocol(trimmedValue)
            else -> _uiState.value.protocol to value
        }

        _uiState.update {
            it.copy(
                protocol = newProtocol,
                baseUrl = newBaseUrl
            )
        }
    }

    /**
     * Splits a URL into the protocol dropdown value and the host text. Defaults to the currently
     * selected protocol when the URL carries none.
     */
    private fun splitProtocol(url: String): Pair<String, String> {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith(HTTPS_PROTOCOL, ignoreCase = true) ->
                HTTPS_PROTOCOL to trimmed.drop(HTTPS_PROTOCOL.length)
            trimmed.startsWith(HTTP_PROTOCOL, ignoreCase = true) ->
                HTTP_PROTOCOL to trimmed.drop(HTTP_PROTOCOL.length)
            else -> _uiState.value.protocol to trimmed
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    private fun getBaseUrl(): String {
        return (_uiState.value.protocol+_uiState.value.baseUrl).trim()
    }

    fun checkLoginSetup() {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) {
            setLoginSetupError(R.string.error_server_url_required)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }

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

        val baseUrl = (currentState.protocol + currentState.baseUrl).trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }
            when (val result = loginUseCase(
                baseUrl = baseUrl,
                username = currentState.username.trim(),
                password = currentState.password
            )) {
                is LoginResult.Success -> {
                    val accessToken = result.data.accessToken?.trim().orEmpty()
                    if (accessToken.isBlank()) {
                        setLoginError(R.string.error_login_failed)
                        return@launch
                    }

                    saveSessionUseCase(
                        LoginSession(
                            accessToken = accessToken,
                            refreshToken = result.data.refreshToken?.trim(),
                            userId = result.data.userId?.trim(),
                            username = currentState.username.trim(),
                            baseUrl = baseUrl
                        )
                    )

                    _uiState.update { it.copy(isLoggingIn = false) }
                    emitMessage(R.string.login_success)
                }
                is LoginResult.Failure -> {
                    setLoginError(toAuthErrorRes(result.error))
                }
            }
        }
    }

    fun clearServerState() {
        _uiState.update {
            it.copy(
                isChecking = false,
                isLoggingIn = false,
                showLoginFields = false,
                serverStatus = null
            )
        }
    }

    private fun setLoginSetupError(messageResId: Int) {
        _uiState.update {
            it.copy(
                isChecking = false,
                isLoggingIn = false,
                showLoginFields = false
            )
        }
        emitMessage(messageResId)
    }

    private fun setLoginError(messageResId: Int) {
        _uiState.update {
            it.copy(
                isChecking = false,
                isLoggingIn = false
            )
        }
        emitMessage(messageResId)
    }

    private fun emitMessage(messageResId: Int) {
        _events.tryEmit(AuthUiEvent.ShowMessage(messageResId))
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

    companion object {
        const val HTTPS_PROTOCOL = "https://"
        const val HTTP_PROTOCOL = "http://"
    }
}
