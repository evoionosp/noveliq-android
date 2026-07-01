package org.evoionosp.noveliq.presentation.splash

data class SplashUiState(
    val isLoading: Boolean = true,
    val startupDestination: StartupDestination = StartupDestination.Auth
)
