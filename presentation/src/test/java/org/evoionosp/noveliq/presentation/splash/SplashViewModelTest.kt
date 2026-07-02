package org.evoionosp.noveliq.presentation.splash

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.noveliq.core.session.LoginSession
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository
import org.evoionosp.noveliq.domain.library.usecase.BootstrapHomeCatalogUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `routes to auth when session is missing`() = runTest(dispatcher) {
        val viewModel = SplashViewModel(
            sessionStore = FakeSessionStore(initialSession = null),
            authRepository = FakeAuthRepository(),
            bootstrapHomeCatalogUseCase = bootstrapUseCase()
        )

        advanceUntilIdle()

        assertEquals(StartupDestination.Auth, viewModel.uiState.value.startupDestination)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun `routes to home when bootstrap succeeds`() = runTest(dispatcher) {
        val session = testSession()
        val viewModel = SplashViewModel(
            sessionStore = FakeSessionStore(initialSession = session),
            authRepository = FakeAuthRepository(),
            bootstrapHomeCatalogUseCase = bootstrapUseCase(
                libraries = listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true)),
                selectedLibrary = AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true),
                libraryRefreshResult = DomainResult.Success(Unit),
                audiobookRefreshResult = DomainResult.Success(Unit)
            )
        )

        advanceUntilIdle()

        assertEquals(StartupDestination.Home(session), viewModel.uiState.value.startupDestination)
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun `routes to authenticated error when bootstrap fails`() = runTest(dispatcher) {
        val session = testSession()
        val viewModel = SplashViewModel(
            sessionStore = FakeSessionStore(initialSession = session),
            authRepository = FakeAuthRepository(),
            bootstrapHomeCatalogUseCase = bootstrapUseCase(
                libraries = emptyList(),
                selectedLibrary = null,
                libraryRefreshResult = DomainResult.Failure(CatalogError.NETWORK),
                audiobookRefreshResult = DomainResult.Success(Unit)
            )
        )

        advanceUntilIdle()

        assertEquals(
            StartupDestination.CatalogLoadError(session, CatalogError.NETWORK),
            viewModel.uiState.value.startupDestination
        )
        assertTrue(!viewModel.uiState.value.isLoading)
    }

    @Test
    fun `retry reruns bootstrap for current session`() = runTest(dispatcher) {
        val session = testSession()
        val audiobookRefreshResult = AtomicReference<DomainResult<Unit>>(DomainResult.Failure(CatalogError.NETWORK))
        val viewModel = SplashViewModel(
            sessionStore = FakeSessionStore(initialSession = session),
            authRepository = FakeAuthRepository(),
            bootstrapHomeCatalogUseCase = bootstrapUseCase(
                libraries = listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true)),
                selectedLibrary = AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true),
                libraryRefreshResult = DomainResult.Success(Unit),
                audiobookRefreshResultRef = audiobookRefreshResult
            )
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.startupDestination is StartupDestination.CatalogLoadError)

        audiobookRefreshResult.set(DomainResult.Success(Unit))
        viewModel.retryCatalogBootstrap()
        advanceUntilIdle()

        assertEquals(StartupDestination.Home(session), viewModel.uiState.value.startupDestination)
    }

    private fun bootstrapUseCase(
        libraries: List<AudiobookLibrary> = emptyList(),
        selectedLibrary: AudiobookLibrary? = null,
        libraryRefreshResult: DomainResult<Unit> = DomainResult.Success(Unit),
        audiobookRefreshResult: DomainResult<Unit> = DomainResult.Success(Unit),
        libraryRefreshResultRef: AtomicReference<DomainResult<Unit>>? = null,
        audiobookRefreshResultRef: AtomicReference<DomainResult<Unit>>? = null
    ): BootstrapHomeCatalogUseCase {
        return BootstrapHomeCatalogUseCase(
            libraryRepository = FakeLibraryRepository(
                libraries = MutableStateFlow(libraries),
                selectedLibrary = MutableStateFlow(selectedLibrary),
                libraryRefreshResult = libraryRefreshResult,
                libraryRefreshResultRef = libraryRefreshResultRef
            ),
            audiobookRepository = FakeAudiobookRepository(
                audiobookRefreshResult = audiobookRefreshResult,
                audiobookRefreshResultRef = audiobookRefreshResultRef
            )
        )
    }

    private fun testSession(): LoginSession {
        return LoginSession(
            accessToken = "token",
            refreshToken = null,
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com"
        )
    }
}

private class FakeAuthRepository(
    private val refreshResult: LoginResult = LoginResult.Failure(AuthError.HTTP)
) : AuthRepository {
    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {
        return LoginResult.Success(
            LoginData(
                accessToken = "token",
                refreshToken = "refresh-token",
                userId = "user-1"
            )
        )
    }

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String
    ): LoginResult = refreshResult
}

private class FakeSessionStore(initialSession: LoginSession?) : SessionStore {
    private val backingFlow = MutableStateFlow(initialSession)

    override val session: Flow<LoginSession?> = backingFlow

    override suspend fun saveSession(session: LoginSession) {
        backingFlow.value = session
    }

    override suspend fun clearSession() {
        backingFlow.value = null
    }
}

private class FakeLibraryRepository(
    private val libraries: MutableStateFlow<List<AudiobookLibrary>>,
    private val selectedLibrary: MutableStateFlow<AudiobookLibrary?>,
    private val libraryRefreshResult: DomainResult<Unit>,
    private val libraryRefreshResultRef: AtomicReference<DomainResult<Unit>>? = null
) : LibraryRepository {
    override fun observeLibraries(): Flow<List<AudiobookLibrary>> = libraries

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> = selectedLibrary

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String
    ): DomainResult<Unit> {
        return libraryRefreshResultRef?.get() ?: libraryRefreshResult
    }

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        val library = libraries.value.firstOrNull { it.id == libraryId }
            ?: return DomainResult.Failure(CatalogError.NOT_FOUND)
        libraries.value = libraries.value.map { item ->
            item.copy(isSelected = item.id == libraryId)
        }
        selectedLibrary.value = library.copy(isSelected = true)
        return DomainResult.Success(Unit)
    }
}

private class FakeAudiobookRepository(
    private val audiobookRefreshResult: DomainResult<Unit>,
    private val audiobookRefreshResultRef: AtomicReference<DomainResult<Unit>>? = null
) : AudiobookRepository {
    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> {
        return MutableStateFlow(emptyList())
    }

    override fun observeAudiobook(libraryId: String, audiobookId: String): Flow<Audiobook?> {
        return MutableStateFlow(null)
    }

    override fun observeAudiobookDetail(libraryId: String, audiobookId: String): Flow<AudiobookDetail?> {
        return MutableStateFlow(null)
    }

    override fun observeContinueListening(libraryId: String): Flow<List<Audiobook>> {
        return MutableStateFlow(emptyList())
    }

    override suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String
    ): DomainResult<Unit> = DomainResult.Success(Unit)

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> {
        return MutableStateFlow(SyncStatus.Idle)
    }

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> = audiobookRefreshResultRef?.get() ?: audiobookRefreshResult

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> = DomainResult.Success(Unit)

    override suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): DomainResult<PlaybackProgress?> = DomainResult.Success(null)

    override suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress
    ): DomainResult<Unit> = DomainResult.Success(Unit)
}
