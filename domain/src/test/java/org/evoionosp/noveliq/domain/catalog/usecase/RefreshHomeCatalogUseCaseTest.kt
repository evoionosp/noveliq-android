package org.evoionosp.noveliq.domain.catalog.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.auth.FakeAuthRepository
import org.evoionosp.noveliq.domain.auth.FakeSessionStore
import org.evoionosp.noveliq.domain.auth.SessionRefreshCoordinator
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.noveliq.domain.testing.FakeAudiobookRepository
import org.evoionosp.noveliq.domain.testing.FakeLibraryRepository
import org.evoionosp.noveliq.domain.testing.testLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshHomeCatalogUseCaseTest {
    private val session =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    @Test
    fun `returns SessionExpired when there is no session`() =
        runTest {
            val graph = graph(session = null)

            assertEquals(RefreshHomeCatalogResult.SessionExpired, graph.useCase())
            assertEquals(0, graph.libraries.refreshCallCount)
        }

    @Test
    fun `refreshes the selected library end to end`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true), testLibrary(id = "lib-2")),
                    selected = testLibrary(isSelected = true),
                )

            assertEquals(RefreshHomeCatalogResult.Success, graph.useCase())

            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedBookLibraries)
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedContinueLibraries)
        }

    @Test
    fun `falls back to the first library when nothing is selected`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(id = "lib-1"), testLibrary(id = "lib-2")),
                    selected = null,
                )

            assertEquals(RefreshHomeCatalogResult.Success, graph.useCase())
            assertEquals(listOf("lib-1"), graph.audiobooks.refreshedBookLibraries)
        }

    @Test
    fun `reports SessionExpired when the library refresh rejects the session`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    libraryRefresh = DomainResult.Failure(CatalogError.AUTH),
                )

            assertEquals(RefreshHomeCatalogResult.SessionExpired, graph.useCase())
        }

    @Test
    fun `reports SessionExpired when the audiobook refresh rejects the session`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    booksRefresh = DomainResult.Failure(CatalogError.AUTH),
                )

            assertEquals(RefreshHomeCatalogResult.SessionExpired, graph.useCase())
        }

    @Test
    fun `reports SessionExpired when continue listening rejects the session`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    continueRefresh = DomainResult.Failure(CatalogError.AUTH),
                )

            assertEquals(RefreshHomeCatalogResult.SessionExpired, graph.useCase())
        }

    @Test
    fun `reports NO_AUDIOBOOK_LIBRARIES when the cache is empty`() =
        runTest {
            val graph = graph(libraries = emptyList(), selected = null)

            assertEquals(
                RefreshHomeCatalogResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES),
                graph.useCase(),
            )
        }

    @Test
    fun `surfaces a non-auth library failure`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    libraryRefresh = DomainResult.Failure(CatalogError.NETWORK),
                )

            assertEquals(
                RefreshHomeCatalogResult.Failure(CatalogError.NETWORK),
                graph.useCase(),
            )
        }

    @Test
    fun `surfaces a non-auth audiobook failure`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    booksRefresh = DomainResult.Failure(CatalogError.NOT_FOUND),
                )

            assertEquals(
                RefreshHomeCatalogResult.Failure(CatalogError.NOT_FOUND),
                graph.useCase(),
            )
        }

    @Test
    fun `tolerates a non-auth continue listening failure`() =
        runTest {
            val graph =
                graph(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selected = testLibrary(isSelected = true),
                    continueRefresh = DomainResult.Failure(CatalogError.NETWORK),
                )

            assertEquals(RefreshHomeCatalogResult.Success, graph.useCase())
        }

    private fun graph(
        session: LoginSession? = this.session,
        libraries: List<org.evoionosp.noveliq.domain.library.model.AudiobookLibrary> = emptyList(),
        selected: org.evoionosp.noveliq.domain.library.model.AudiobookLibrary? = null,
        libraryRefresh: DomainResult<Unit> = DomainResult.Success(Unit),
        booksRefresh: DomainResult<Unit> = DomainResult.Success(Unit),
        continueRefresh: DomainResult<Unit> = DomainResult.Success(Unit),
    ): Graph {
        val store = FakeSessionStore(session)
        val auth = FakeAuthRepository(LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")))
        val libraryRepository =
            FakeLibraryRepository(
                libraries = libraries,
                selectedLibrary = selected,
                refreshResult = libraryRefresh,
            )
        val audiobookRepository =
            FakeAudiobookRepository(
                refreshBooksResult = booksRefresh,
                refreshContinueResult = continueRefresh,
            )
        val useCase =
            RefreshHomeCatalogUseCase(
                getValidSessionUseCase = GetValidSessionUseCase(store, SessionRefreshCoordinator(store, auth)),
                refreshLibrariesUseCase = RefreshLibrariesUseCase(libraryRepository),
                observeSelectedLibraryUseCase = ObserveSelectedLibraryUseCase(libraryRepository),
                observeLibrariesUseCase = ObserveLibrariesUseCase(libraryRepository),
                refreshSelectedLibraryAudiobooksUseCase = RefreshSelectedLibraryAudiobooksUseCase(audiobookRepository),
                refreshContinueListeningUseCase = RefreshContinueListeningUseCase(audiobookRepository),
            )
        return Graph(useCase, libraryRepository, audiobookRepository)
    }

    private data class Graph(
        val useCase: RefreshHomeCatalogUseCase,
        val libraries: FakeLibraryRepository,
        val audiobooks: FakeAudiobookRepository,
    )
}
