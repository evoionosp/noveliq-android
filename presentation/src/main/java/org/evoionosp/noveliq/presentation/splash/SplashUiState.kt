package org.evoionosp.noveliq.presentation.splash

import org.evoionosp.noveliq.common.session.LoginSession

data class SplashUiState(
    val isLoading: Boolean = true,
    val session: LoginSession? = null,
    val isLibraryLoaded: Boolean = false
)
