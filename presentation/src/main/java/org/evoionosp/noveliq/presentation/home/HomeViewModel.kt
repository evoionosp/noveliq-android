package org.evoionosp.noveliq.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.LoginSession
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveHomeAudiobooksUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveLibrarySyncStatusUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.SelectLibraryUseCase
import org.evoionosp.noveliq.presentation.R

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val observeLibrariesUseCase: ObserveLibrariesUseCase,
    private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
    private val observeHomeAudiobooksUseCase: ObserveHomeAudiobooksUseCase,
    private val observeContinueListeningUseCase: ObserveContinueListeningUseCase,
    private val observeLibrarySyncStatusUseCase: ObserveLibrarySyncStatusUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
    private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
    private val selectLibraryUseCase: SelectLibraryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeLibrariesUseCase().collect { libraries ->
                _uiState.update { it.copy(libraries = libraries) }
            }
        }

        viewModelScope.launch {
            observeSelectedLibraryUseCase().collect { selectedLibrary ->
                _uiState.update {
                    it.copy(
                        selectedLibraryId = selectedLibrary?.id,
                        selectedLibraryName = selectedLibrary?.name
                    )
                }
            }
        }

        viewModelScope.launch {
            observeSelectedLibraryUseCase()
                .map { it?.id }
                .distinctUntilChanged()
                .flatMapLatest { libraryId ->
                    if (libraryId == null) emptyFlow() else observeHomeAudiobooksUseCase(libraryId)
                }
                .collectLatest { audiobooks ->
                    _uiState.update { it.copy(audiobooks = audiobooks) }
                }
        }

        viewModelScope.launch {
            observeSelectedLibraryUseCase()
                .map { it?.id }
                .distinctUntilChanged()
                .flatMapLatest { libraryId ->
                    if (libraryId == null) emptyFlow() else observeContinueListeningUseCase(libraryId)
                }
                .collectLatest { continueListening ->
                    _uiState.update { it.copy(continueListening = continueListening) }
                }
        }

        viewModelScope.launch {
            observeSelectedLibraryUseCase()
                .map { it?.id }
                .distinctUntilChanged()
                .flatMapLatest { libraryId ->
                    if (libraryId == null) emptyFlow() else observeLibrarySyncStatusUseCase(libraryId)
                }
                .collectLatest { syncStatus ->
                    _uiState.update { it.copy(syncStatus = syncStatus) }
                }
        }
    }

    fun onLibrarySelected(libraryId: String) {
        if (_uiState.value.selectedLibraryId == libraryId) return

        viewModelScope.launch {
            when (selectLibraryUseCase(libraryId)) {
                is DomainResult.Success -> {
                    val session = sessionStore.session.first() ?: return@launch
                    val refreshResult = refreshSelectedLibraryAudiobooks(
                        session = session,
                        libraryId = libraryId
                    )
                    val continueResult = refreshContinueListening(
                        session = sessionStore.session.first() ?: session,
                        libraryId = libraryId
                    )
                    if (refreshResult.isAuthFailure() || continueResult.isAuthFailure()) {
                        handleAuthFailure()
                    }
                }
                is DomainResult.Failure -> {
                    emitMessage(R.string.error_home_library_select)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val session = sessionStore.session.first() ?: return@launch
            _uiState.update { it.copy(isRefreshing = true) }

            var activeSession = session
            var libraryRefreshResult = refreshLibraries(activeSession)
            if (libraryRefreshResult.isAuthFailure()) {
                activeSession = refreshSessionOrExpire() ?: run {
                    _uiState.update { it.copy(isRefreshing = false) }
                    expireSession()
                    return@launch
                }
                libraryRefreshResult = refreshLibraries(activeSession)
            }

            val selectedLibraryId = observeSelectedLibraryUseCase().first()?.id
                ?: observeLibrariesUseCase().first().firstOrNull()?.id

            val audiobookRefreshResult = if (selectedLibraryId != null) {
                var result = refreshSelectedLibraryAudiobooks(activeSession, selectedLibraryId)
                if (result.isAuthFailure()) {
                    activeSession = refreshSessionOrExpire() ?: run {
                        _uiState.update { it.copy(isRefreshing = false) }
                        expireSession()
                        return@launch
                    }
                    result = refreshSelectedLibraryAudiobooks(activeSession, selectedLibraryId)
                }
                val continueResult = refreshContinueListening(activeSession, selectedLibraryId)
                if (continueResult.isAuthFailure()) {
                    activeSession = refreshSessionOrExpire() ?: run {
                        _uiState.update { it.copy(isRefreshing = false) }
                        expireSession()
                        return@launch
                    }
                    refreshContinueListening(activeSession, selectedLibraryId)
                }
                result
            } else {
                DomainResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES)
            }

            _uiState.update { it.copy(isRefreshing = false) }
            when {
                libraryRefreshResult.isAuthFailure() || audiobookRefreshResult.isAuthFailure() -> handleAuthFailure()
                libraryRefreshResult is DomainResult.Failure -> emitMessage(toMessageRes(libraryRefreshResult.error))
                audiobookRefreshResult is DomainResult.Failure -> emitMessage(toMessageRes(audiobookRefreshResult.error))
                else -> emitMessage(R.string.home_refresh_complete)
            }
        }
    }

    fun clearExpiredSession() {
        viewModelScope.launch {
            sessionStore.clearSession()
        }
    }

    private fun emitMessage(messageResId: Int) {
        _events.tryEmit(HomeUiEvent.ShowMessage(messageResId))
    }

    private suspend fun handleAuthFailure() {
        if (refreshSessionOrExpire() == null) {
            expireSession()
        }
    }

    private fun expireSession() {
        _events.tryEmit(HomeUiEvent.SessionExpired)
        viewModelScope.launch {
            sessionStore.clearSession()
        }
    }

    private suspend fun refreshSessionOrExpire(): LoginSession? {
        val currentSession = sessionStore.session.first() ?: return null
        val refreshToken = currentSession.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        return when (
            val result = authRepository.refreshSession(
                baseUrl = currentSession.baseUrl,
                refreshToken = refreshToken
            )
        ) {
            is LoginResult.Success -> {
                val accessToken = result.data.accessToken?.trim().orEmpty()
                if (accessToken.isBlank()) return null
                val updatedSession = currentSession.copy(
                    accessToken = accessToken,
                    refreshToken = result.data.refreshToken?.trim()?.takeIf { it.isNotBlank() }
                        ?: currentSession.refreshToken,
                    userId = result.data.userId?.trim() ?: currentSession.userId
                )
                sessionStore.saveSession(updatedSession)
                updatedSession
            }
            is LoginResult.Failure -> null
        }
    }

    private suspend fun refreshLibraries(session: LoginSession): DomainResult<Unit> {
        return refreshLibrariesUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken
        )
    }

    private suspend fun refreshSelectedLibraryAudiobooks(
        session: LoginSession,
        libraryId: String
    ): DomainResult<Unit> {
        return refreshSelectedLibraryAudiobooksUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            libraryId = libraryId
        )
    }

    private suspend fun refreshContinueListening(
        session: LoginSession,
        libraryId: String
    ): DomainResult<Unit> {
        return refreshContinueListeningUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            libraryId = libraryId
        )
    }

    private fun DomainResult<Unit>.isAuthFailure(): Boolean {
        return this is DomainResult.Failure && error == CatalogError.AUTH
    }

    private fun toMessageRes(error: CatalogError): Int {
        return when (error) {
            CatalogError.AUTH -> R.string.error_login_failed
            CatalogError.NETWORK -> R.string.error_network
            CatalogError.CONNECTIVITY_UNAVAILABLE -> R.string.error_network
            CatalogError.NO_AUDIOBOOK_LIBRARIES -> R.string.home_no_libraries
            CatalogError.NOT_FOUND -> R.string.error_home_library_select
            CatalogError.UNKNOWN -> R.string.error_unknown
        }
    }
}
