package org.evoionosp.noveliq.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.LoginSession
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.noveliq.domain.library.usecase.BootstrapHomeCatalogUseCase

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val bootstrapHomeCatalogUseCase: BootstrapHomeCatalogUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    private var currentSession: LoginSession? = null

    init {
        viewModelScope.launch {
            sessionStore.session.collectLatest { session ->
                currentSession = session
                if (session == null) {
                    _uiState.value = SplashUiState(
                        isLoading = false,
                        startupDestination = StartupDestination.Auth
                    )
                    return@collectLatest
                }

                bootstrapCatalog(session)
            }
        }
    }

    fun retryCatalogBootstrap() {
        val session = currentSession ?: return
        viewModelScope.launch {
            bootstrapCatalog(session)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionStore.clearSession()
        }
    }

    private suspend fun bootstrapCatalog(session: LoginSession) {
        _uiState.value = SplashUiState(isLoading = true)

        val bootstrapResult = bootstrapHomeCatalogUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken
        )

        currentCoroutineContext().ensureActive()

        _uiState.value = SplashUiState(
            isLoading = false,
            startupDestination = when (bootstrapResult) {
                is BootstrapHomeCatalogResult.Success,
                BootstrapHomeCatalogResult.NoLibrariesAvailable -> StartupDestination.Home(session)
                is BootstrapHomeCatalogResult.Failure -> StartupDestination.CatalogLoadError(
                    session = session,
                    error = bootstrapResult.error
                )
            }
        )
    }
}
