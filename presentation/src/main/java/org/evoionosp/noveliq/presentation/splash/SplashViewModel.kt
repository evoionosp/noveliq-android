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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.LoginSession
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.usecase.BootstrapHomeCatalogUseCase

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val bootstrapHomeCatalogUseCase: BootstrapHomeCatalogUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    private var currentSession: LoginSession? = null

    init {
        viewModelScope.launch {
            sessionStore.session
                // Always keep the latest session (including rotated tokens) for retries.
                .onEach { currentSession = it }
                // Only react to routing-relevant changes (login/logout/user switch).
                // A silent access-token rotation must NOT re-trigger the bootstrap, or the
                // NavHost briefly rebuilds at the Auth route and flashes the login screen.
                .distinctUntilChanged { old, new -> old.isSameRouting(new) }
                .collectLatest { session ->
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

        // Keep the token embedded in the current destination fresh when it rotates, without
        // re-running the bootstrap. This propagates the new access token to authenticated
        // image/media loading while the NavHost stays put (it keys on the route only).
        viewModelScope.launch {
            sessionStore.session.collect { session ->
                if (session == null) return@collect
                _uiState.update { state ->
                    when (val destination = state.startupDestination) {
                        is StartupDestination.Home ->
                            if (destination.session == session) state
                            else state.copy(startupDestination = StartupDestination.Home(session))
                        is StartupDestination.CatalogLoadError ->
                            if (destination.session == session) state
                            else state.copy(startupDestination = destination.copy(session = session))
                        StartupDestination.Auth -> state
                    }
                }
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
        // Preserve the current destination while loading. Constructing a fresh
        // SplashUiState() would reset startupDestination to its default (Auth) and
        // flash the login screen during any re-bootstrap.
        _uiState.update { it.copy(isLoading = true) }

        var activeSession = session
        var bootstrapResult = bootstrapHomeCatalogUseCase(
            baseUrl = activeSession.baseUrl,
            accessToken = activeSession.accessToken
        )

        if (bootstrapResult is BootstrapHomeCatalogResult.Failure && bootstrapResult.error == CatalogError.AUTH) {
            activeSession = refreshSession(session) ?: session
            if (activeSession.accessToken != session.accessToken) {
                bootstrapResult = bootstrapHomeCatalogUseCase(
                    baseUrl = activeSession.baseUrl,
                    accessToken = activeSession.accessToken
                )
            }
        }

        currentCoroutineContext().ensureActive()

        _uiState.value = SplashUiState(
            isLoading = false,
            startupDestination = when (bootstrapResult) {
                is BootstrapHomeCatalogResult.Success,
                BootstrapHomeCatalogResult.NoLibrariesAvailable -> StartupDestination.Home(activeSession)
                is BootstrapHomeCatalogResult.Failure -> StartupDestination.CatalogLoadError(
                    session = activeSession,
                    error = bootstrapResult.error
                )
            }
        )
    }

    private suspend fun refreshSession(session: LoginSession): LoginSession? {
        val refreshToken = session.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        return when (
            val result = authRepository.refreshSession(
                baseUrl = session.baseUrl,
                refreshToken = refreshToken
            )
        ) {
            is LoginResult.Success -> {
                val accessToken = result.data.accessToken?.trim().orEmpty()
                if (accessToken.isBlank()) return null
                val updatedSession = session.copy(
                    accessToken = accessToken,
                    refreshToken = result.data.refreshToken?.trim()?.takeIf { it.isNotBlank() }
                        ?: session.refreshToken,
                    userId = result.data.userId?.trim() ?: session.userId
                )
                sessionStore.saveSession(updatedSession)
                updatedSession
            }
            is LoginResult.Failure -> null
        }
    }

    /**
     * Two sessions route to the same startup destination when they refer to the same
     * logged-in user on the same server. Deliberately ignores access/refresh tokens so a
     * silent token rotation does not look like a new session.
     */
    private fun LoginSession?.isSameRouting(other: LoginSession?): Boolean {
        if (this == null || other == null) return this == null && other == null
        return baseUrl == other.baseUrl && username == other.username && userId == other.userId
    }
}
