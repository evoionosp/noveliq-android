package org.evoionosp.noveliq.domain.library.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapHomeCatalogUseCaseTest {
    @Test
    fun `bootstrap reuses selected library and refreshes audiobooks`() = runTest {
        val libraries = MutableStateFlow(
            listOf(
                AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true),
                AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false)
            )
        )
        val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
        val audiobooks = MutableStateFlow(emptyList<Audiobook>())
        val libraryRepository = FakeLibraryRepository(
            libraries = libraries,
            selectedLibrary = selected,
            refreshResult = DomainResult.Success(Unit)
        )
        val audiobookRepository = FakeAudiobookRepository(
            audiobooksByLibraryId = mutableMapOf("lib-1" to audiobooks),
            refreshResult = DomainResult.Success(Unit)
        )

        val result = BootstrapHomeCatalogUseCase(
            libraryRepository = libraryRepository,
            audiobookRepository = audiobookRepository
        )(
            baseUrl = "https://example.com",
            accessToken = "token"
        )

        assertEquals(
            BootstrapHomeCatalogResult.Success(
                selectedLibraryId = "lib-1",
                usedCachedData = false
            ),
            result
        )
        assertEquals(listOf("lib-1"), audiobookRepository.refreshedLibraries)
    }

    @Test
    fun `bootstrap falls back to first library when selection missing`() = runTest {
        val libraries = MutableStateFlow(
            listOf(
                AudiobookLibrary(id = "lib-1", name = "Main", isSelected = false),
                AudiobookLibrary(id = "lib-2", name = "Alt", isSelected = false)
            )
        )
        val selected = MutableStateFlow<AudiobookLibrary?>(null)
        val libraryRepository = FakeLibraryRepository(
            libraries = libraries,
            selectedLibrary = selected,
            refreshResult = DomainResult.Success(Unit)
        )
        val audiobookRepository = FakeAudiobookRepository(
            audiobooksByLibraryId = mutableMapOf("lib-1" to MutableStateFlow(emptyList())),
            refreshResult = DomainResult.Success(Unit)
        )

        val result = BootstrapHomeCatalogUseCase(libraryRepository, audiobookRepository)(
            baseUrl = "https://example.com",
            accessToken = "token"
        )

        assertEquals(BootstrapHomeCatalogResult.Success("lib-1", false), result)
        assertEquals("lib-1", libraryRepository.selectedLibrary.value?.id)
    }

    @Test
    fun `bootstrap returns cached success when audiobook refresh fails but cache exists`() = runTest {
        val libraries = MutableStateFlow(
            listOf(AudiobookLibrary(id = "lib-1", name = "Main", isSelected = true))
        )
        val selected = MutableStateFlow<AudiobookLibrary?>(libraries.value.first())
        val cachedBooks = MutableStateFlow(
            listOf(
                Audiobook(
                    id = "book-1",
                    libraryId = "lib-1",
                    title = "Title",
                    author = "Author",
                    coverUrl = "https://example.com/cover",
                    series = null,
                    durationInSeconds = null
                )
            )
        )
        val result = BootstrapHomeCatalogUseCase(
            libraryRepository = FakeLibraryRepository(libraries, selected, DomainResult.Success(Unit)),
            audiobookRepository = FakeAudiobookRepository(
                audiobooksByLibraryId = mutableMapOf("lib-1" to cachedBooks),
                refreshResult = DomainResult.Failure(CatalogError.NETWORK)
            )
        )(
            baseUrl = "https://example.com",
            accessToken = "token"
        )

        assertEquals(BootstrapHomeCatalogResult.Success("lib-1", true), result)
    }

    @Test
    fun `bootstrap returns no libraries when sync leaves library cache empty`() = runTest {
        val result = BootstrapHomeCatalogUseCase(
            libraryRepository = FakeLibraryRepository(
                libraries = MutableStateFlow(emptyList()),
                selectedLibrary = MutableStateFlow(null),
                refreshResult = DomainResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES)
            ),
            audiobookRepository = FakeAudiobookRepository(
                audiobooksByLibraryId = mutableMapOf(),
                refreshResult = DomainResult.Success(Unit)
            )
        )(
            baseUrl = "https://example.com",
            accessToken = "token"
        )

        assertTrue(result is BootstrapHomeCatalogResult.NoLibrariesAvailable)
    }
}

private class FakeLibraryRepository(
    val libraries: MutableStateFlow<List<AudiobookLibrary>>,
    val selectedLibrary: MutableStateFlow<AudiobookLibrary?>,
    private val refreshResult: DomainResult<Unit>
) : LibraryRepository {
    override fun observeLibraries(): Flow<List<AudiobookLibrary>> = libraries

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> = selectedLibrary

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String
    ): DomainResult<Unit> = refreshResult

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        val library = libraries.value.firstOrNull { it.id == libraryId }
            ?: return DomainResult.Failure(CatalogError.NOT_FOUND)
        val updatedLibraries = libraries.value.map { item ->
            item.copy(isSelected = item.id == libraryId)
        }
        libraries.value = updatedLibraries
        selectedLibrary.value = library.copy(isSelected = true)
        return DomainResult.Success(Unit)
    }
}

private class FakeAudiobookRepository(
    val audiobooksByLibraryId: MutableMap<String, MutableStateFlow<List<Audiobook>>>,
    private val refreshResult: DomainResult<Unit>
) : AudiobookRepository {
    val refreshedLibraries = mutableListOf<String>()
    val refreshedContinueListeningLibraries = mutableListOf<String>()

    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> {
        return audiobooksByLibraryId.getOrPut(libraryId) { MutableStateFlow(emptyList()) }
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
    ): DomainResult<Unit> {
        refreshedLibraries += libraryId
        return refreshResult
    }

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> {
        refreshedContinueListeningLibraries += libraryId
        return DomainResult.Success(Unit)
    }

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
