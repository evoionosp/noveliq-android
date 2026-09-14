package org.evoionosp.noveliq.domain.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.evoionosp.noveliq.domain.server.repository.ServerRepository

/**
 * Shared fakes for domain unit tests. Prefer these over mocks: they model the
 * repository as observable state (MutableStateFlow) plus scriptable results,
 * which is how the real implementations behave from a use case's perspective.
 */
class FakeLibraryRepository(
    libraries: List<AudiobookLibrary> = emptyList(),
    selectedLibrary: AudiobookLibrary? = null,
    var refreshResult: DomainResult<Unit> = DomainResult.Success(Unit),
    /** When non-null, returned verbatim instead of updating the selection state. */
    var selectResultOverride: DomainResult<Unit>? = null,
) : LibraryRepository {
    val librariesFlow = MutableStateFlow(libraries)
    val selectedLibraryFlow = MutableStateFlow(selectedLibrary)
    var refreshCallCount = 0
        private set
    val selectedLibraryIds = mutableListOf<String>()

    override fun observeLibraries(): Flow<List<AudiobookLibrary>> = librariesFlow

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> = selectedLibraryFlow

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String,
    ): DomainResult<Unit> {
        refreshCallCount++
        return refreshResult
    }

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        selectedLibraryIds += libraryId
        selectResultOverride?.let { return it }
        val library =
            librariesFlow.value.firstOrNull { it.id == libraryId }
                ?: return DomainResult.Failure(org.evoionosp.noveliq.domain.library.model.CatalogError.NOT_FOUND)
        librariesFlow.value = librariesFlow.value.map { it.copy(isSelected = it.id == libraryId) }
        selectedLibraryFlow.value = library.copy(isSelected = true)
        return DomainResult.Success(Unit)
    }
}

class FakeAudiobookRepository(
    var refreshBooksResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var refreshContinueResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var refreshDetailResult: DomainResult<Unit> = DomainResult.Success(Unit),
    var fetchProgressResult: DomainResult<PlaybackProgress?> = DomainResult.Success(null),
    var saveProgressResult: DomainResult<Unit> = DomainResult.Success(Unit),
    /**
     * Runs when [refreshAudiobookDetail] succeeds, so tests can model the server
     * populating the cache that [observeAudiobookDetail] reads back.
     */
    var onRefreshDetail: (suspend (libraryId: String, audiobookId: String) -> Unit)? = null,
) : AudiobookRepository {
    private val booksFlows = mutableMapOf<String, MutableStateFlow<List<Audiobook>>>()
    private val detailFlows = mutableMapOf<Pair<String, String>, MutableStateFlow<AudiobookDetail?>>()

    var continueListening: List<Audiobook> = emptyList()
    var syncStatus: SyncStatus = SyncStatus.Idle

    val refreshedBookLibraries = mutableListOf<String>()
    val refreshedContinueLibraries = mutableListOf<String>()
    val refreshedDetails = mutableListOf<Pair<String, String>>()
    val savedProgress = mutableListOf<PlaybackProgress>()

    fun setAudiobooks(
        libraryId: String,
        books: List<Audiobook>,
    ) {
        booksFlows.getOrPut(libraryId) { MutableStateFlow(emptyList()) }.value = books
    }

    fun setDetail(
        libraryId: String,
        audiobookId: String,
        detail: AudiobookDetail?,
    ) {
        detailFlows.getOrPut(libraryId to audiobookId) { MutableStateFlow(null) }.value = detail
    }

    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> =
        booksFlows.getOrPut(libraryId) { MutableStateFlow(emptyList()) }

    override fun observeAudiobook(
        libraryId: String,
        audiobookId: String,
    ): Flow<Audiobook?> = MutableStateFlow(null)

    override fun observeAudiobookDetail(
        libraryId: String,
        audiobookId: String,
    ): Flow<AudiobookDetail?> = detailFlows.getOrPut(libraryId to audiobookId) { MutableStateFlow(null) }

    override fun observeContinueListening(libraryId: String): Flow<List<Audiobook>> = MutableStateFlow(continueListening)

    override suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String,
    ): DomainResult<Unit> {
        refreshedDetails += libraryId to audiobookId
        if (refreshDetailResult is DomainResult.Success) {
            onRefreshDetail?.invoke(libraryId, audiobookId)
        }
        return refreshDetailResult
    }

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> = MutableStateFlow(syncStatus)

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedBookLibraries += libraryId
        return refreshBooksResult
    }

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
    ): DomainResult<Unit> {
        refreshedContinueLibraries += libraryId
        return refreshContinueResult
    }

    override suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
    ): DomainResult<PlaybackProgress?> = fetchProgressResult

    override suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress,
    ): DomainResult<Unit> {
        savedProgress += progress
        return saveProgressResult
    }
}

class FakeServerRepository(
    var pingResult: ServerCheckResult<Boolean> = ServerCheckResult.Success(true),
    var statusResult: ServerCheckResult<ServerStatus> =
        ServerCheckResult.Success(
            ServerStatus(
                app = "audiobookshelf",
                serverVersion = "2.0.0",
                isInit = true,
                language = "en-US",
                authMethods = listOf("local"),
                authLoginCustomMessage = "",
            ),
        ),
    var healthResult: ServerCheckResult<Boolean> = ServerCheckResult.Success(true),
) : ServerRepository {
    var pingCallCount = 0
        private set
    var statusCallCount = 0
        private set
    var healthCallCount = 0
        private set

    override suspend fun ping(baseUrl: String): ServerCheckResult<Boolean> {
        pingCallCount++
        return pingResult
    }

    override suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus> {
        statusCallCount++
        return statusResult
    }

    override suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean> {
        healthCallCount++
        return healthResult
    }
}

class FakeConnectivityObserver(
    initiallyConnected: Boolean = false,
) : ConnectivityObserver {
    private val connected = MutableStateFlow(initiallyConnected)

    override fun observe(): Flow<Boolean> = connected

    override fun isConnected(): Boolean = connected.value

    fun setConnected(value: Boolean) {
        connected.value = value
    }
}

fun testLibrary(
    id: String = "lib-1",
    name: String = "Library $id",
    isSelected: Boolean = false,
) = AudiobookLibrary(
    id = id,
    name = name,
    isSelected = isSelected,
)

fun testAudiobook(
    id: String = "book-1",
    libraryId: String = "lib-1",
) = Audiobook(
    id = id,
    libraryId = libraryId,
    title = "Title $id",
    author = "Author",
    coverUrl = "https://example.com/$id.jpg",
    series = null,
    durationInSeconds = 300,
)

fun testTrack(
    index: Int = 0,
    startOffsetInSeconds: Long = 0,
    durationInSeconds: Long = 300,
) = AudiobookTrack(
    index = index,
    startOffsetInSeconds = startOffsetInSeconds,
    durationInSeconds = durationInSeconds,
    title = "Track $index",
    remoteUrl = "https://example.com/$index.mp3",
    mimeType = "audio/mpeg",
)

fun testDetail(
    audiobook: Audiobook = testAudiobook(),
    tracks: List<AudiobookTrack> = listOf(testTrack()),
    chapters: List<AudiobookChapter> = emptyList(),
) = AudiobookDetail(
    audiobook = audiobook,
    description = null,
    chapters = chapters,
    tracks = tracks,
    refreshedAtMillis = 0L,
)
