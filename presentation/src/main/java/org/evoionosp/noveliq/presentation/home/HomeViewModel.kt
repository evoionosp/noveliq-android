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
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveContinueListeningUseCase
import org.evoionosp.noveliq.domain.session.usecase.GetCurrentSessionUseCase
import org.evoionosp.noveliq.domain.session.usecase.ClearSessionUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveHomeAudiobooksUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveLibrarySyncStatusUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.catalog.usecase.RefreshHomeCatalogUseCase
import org.evoionosp.noveliq.domain.catalog.usecase.RefreshHomeCatalogResult
import org.evoionosp.noveliq.domain.auth.usecase.RefreshSessionUseCase
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.SelectLibraryUseCase
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.common.model.toUiModel
import org.evoionosp.noveliq.presentation.common.model.AuthorUiModel
import org.evoionosp.noveliq.domain.session.usecase.ObserveSessionUseCase

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val clearSessionUseCase: ClearSessionUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val observeLibrariesUseCase: ObserveLibrariesUseCase,
    private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
    private val observeHomeAudiobooksUseCase: ObserveHomeAudiobooksUseCase,
    private val observeContinueListeningUseCase: ObserveContinueListeningUseCase,
    private val observeLibrarySyncStatusUseCase: ObserveLibrarySyncStatusUseCase,
    private val refreshHomeCatalogUseCase: RefreshHomeCatalogUseCase,
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
            observeSessionUseCase().collect { session ->
                val username = session?.username ?: ""
                val firstName = username.substringBefore(" ").substringBefore("@").ifBlank { username }
                _uiState.update { it.copy(username = firstName) }
            }
        }

        viewModelScope.launch {
            observeLibrariesUseCase().collect { libraries ->
                _uiState.update { it.copy(libraries = libraries.map { lib -> lib.toUiModel() }) }
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
                    val uiModels = audiobooks.map { it.toUiModel() }
                    val recentlyAdded = uiModels.take(12)
                    val discover = uiModels
                        .sortedBy { it.title.lowercase() }
                        .filterIndexed { index, _ -> index % 2 == 0 }
                        .take(12)
                    val authors = uiModels
                        .flatMap { audiobook ->
                            audiobook.authorNames.map { authorName -> authorName to audiobook }
                        }
                        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                        .map { (author, books) ->
                            AuthorUiModel(
                                name = author,
                                bookCount = books.size,
                                photoUrl = books.firstOrNull()?.coverUrl
                            )
                        }
                        .sortedBy { it.name.lowercase() }
                    val authorCount = authors.size
                    val durationHours = uiModels.sumOf { it.durationInSeconds ?: 0L } / 3600.0

                    _uiState.update {
                        it.copy(
                            audiobooks = uiModels,
                            recentlyAdded = recentlyAdded,
                            discover = discover,
                            authors = authors,
                            authorCount = authorCount,
                            durationHours = durationHours
                        )
                    }
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
                    _uiState.update { it.copy(continueListening = continueListening.map { it.toUiModel() }) }
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
                    val session = getCurrentSessionUseCase() ?: return@launch
                    val refreshResult = refreshSelectedLibraryAudiobooks(
                        session = session,
                        libraryId = libraryId
                    )
                    val continueResult = refreshContinueListening(
                        session = getCurrentSessionUseCase() ?: session,
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

    /** Manual pull-to-refresh: shows the refresh indicator and reports the outcome. */
    fun refresh() {
        performRefresh(silent = false)
    }

    /**
     * Refresh triggered when the app returns to the foreground. Fetches the latest content
     * without showing the pull-to-refresh indicator and without surfacing success/network
     * toasts. A genuinely expired session still routes the user to login.
     */
    fun refreshSilently() {
        performRefresh(silent = true)
    }

    private fun performRefresh(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            when (val result = refreshHomeCatalogUseCase()) {
                is RefreshHomeCatalogResult.Success -> {
                    if (!silent) {
                        emitMessage(R.string.home_refresh_complete)
                    }
                }
                is RefreshHomeCatalogResult.SessionExpired -> {
                    stopRefreshing(silent)
                    expireSession()
                    return@launch
                }
                is RefreshHomeCatalogResult.Failure -> {
                    if (!silent) {
                        emitMessage(toMessageRes(result.error))
                    }
                }
            }

            stopRefreshing(silent)
        }
    }

    private fun stopRefreshing(silent: Boolean) {
        if (!silent) {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearExpiredSession() {
        viewModelScope.launch {
            clearSessionUseCase()
        }
    }

    private fun emitMessage(messageResId: Int) {
        _events.tryEmit(HomeUiEvent.ShowMessage(messageResId))
    }

    private suspend fun handleAuthFailure() {
        if (refreshSessionUseCase() == null) {
            expireSession()
        }
    }

    private fun expireSession() {
        _events.tryEmit(HomeUiEvent.SessionExpired)
        viewModelScope.launch {
            clearSessionUseCase()
        }
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
