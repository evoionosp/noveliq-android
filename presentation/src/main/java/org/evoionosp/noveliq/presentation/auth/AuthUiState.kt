package org.evoionosp.noveliq.presentation.auth

import org.evoionosp.noveliq.domain.server.model.ServerStatus

data class AuthUiState(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isChecking: Boolean = false,
    val isLoggingIn: Boolean = false,
    val showLoginFields: Boolean = false,
    val serverStatus: ServerStatus? = null
)
