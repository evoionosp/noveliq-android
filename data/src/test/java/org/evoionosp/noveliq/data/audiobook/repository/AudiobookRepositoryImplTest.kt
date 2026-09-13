package org.evoionosp.noveliq.data.audiobook.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.test.FakeConnectivityObserver
import org.evoionosp.noveliq.data.test.MockWebServerRule
import org.evoionosp.noveliq.data.test.TestDatabase
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudiobookRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val testDispatcher = StandardTestDispatcher()
    private val connectivity = FakeConnectivityObserver()

    private lateinit var database: NoveliqDatabase
    private lateinit var repository: AudiobookRepositoryImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        repository =
            AudiobookRepositoryImpl(
                database = database,
                audiobookDao = database.audiobookDao(),
                audiobookDetailDao = database.audiobookDetailDao(),
                continueListeningDao = database.continueListeningDao(),
                syncStateDao = database.librarySyncStateDao(),
                serviceFactory = AudiobookshelfLibraryServiceFactory(OkHttpClient()),
                connectivityObserver = connectivity,
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `refreshAudiobooks offline fails and marks sync failed without cache`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)

            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )

            assertEquals(DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE), result)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("FAILED", sync.status)
            assertEquals("CONNECTIVITY_UNAVAILABLE", sync.error)
        }

    @Test
    fun `refreshAudiobooks offline marks sync stale when cache exists`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)
            database.audiobookDao().upsertAudiobooks(listOf(cachedBook()))

            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )

            assertEquals(DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE), result)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("STALE", sync.status)
        }

    @Test
    fun `refreshAudiobooks success replaces library items and marks sync success`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [${bookJson(id = "item1")}, ${bookJson(id = "item2")}]}""",
            )

            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )

            assertEquals(DomainResult.Success(Unit), result)
            val cached = database.audiobookDao().getAudiobooks("lib1")
            assertEquals(listOf("item1", "item2"), cached.map { it.id })
            assertEquals("Frank Herbert", cached.first().author)
            val sync = database.librarySyncStateDao().getSyncState("lib1")!!
            assertEquals("SUCCESS", sync.status)
            assertNotNull(sync.lastSyncedAtMillis)
        }

    @Test
    fun `refreshAudiobooks propagates auth errors without cache fallback`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(401, "{}")

            val result =
                repository.refreshAudiobooks(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale-token",
                    libraryId = "lib1",
                )

            assertEquals(DomainResult.Failure(CatalogError.AUTH), result)
        }

    @Test
    fun `refreshAudiobookDetail caches detail chapters and tracks`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, fullBookJson())

            val result =
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "item1",
                )

            assertEquals(DomainResult.Success(Unit), result)
            repository.observeAudiobookDetail("lib1", "item1").filterNotNull().test {
                val detail = awaitItem()
                assertEquals("Dune", detail.audiobook.title)
                assertEquals("Desert epic.", detail.description)
                assertEquals(listOf("One", "Two"), detail.chapters.map { it.title })
                assertEquals(listOf(0, 1), detail.tracks.map { it.index })
                assertTrue(
                    detail.tracks
                        .single { it.index == 0 }
                        .remoteUrl
                        .endsWith("/audio/1.mp3"),
                )
            }
        }

    @Test
    fun `refreshAudiobookDetail maps not found and rejects non-books`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "missing",
                ),
            )

            serverRule.enqueueJson(200, bookJson(id = "pod1", mediaType = "podcast"))
            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.refreshAudiobookDetail(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                    audiobookId = "pod1",
                ),
            )
        }

    @Test
    fun `refreshContinueListening caches only the requested library`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"results": [
                    ${bookJson(id = "item1", libraryId = "lib1")},
                    ${bookJson(id = "item9", libraryId = "lib2")}
                ]}""",
            )

            val result =
                repository.refreshContinueListening(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    libraryId = "lib1",
                )

            assertEquals(DomainResult.Success(Unit), result)
            repository.observeContinueListening("lib1").test {
                assertEquals(listOf("item1"), awaitItem().map { it.id })
            }
            repository.observeContinueListening("lib2").test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `fetchProgress maps server progress`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"currentTime": 120.5, "duration": 600.0, "isFinished": false}""",
            )

            val result =
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                )

            assertEquals(
                DomainResult.Success(PlaybackProgress(120.5, 600.0, false)),
                result,
            )
        }

    @Test
    fun `fetchProgress returns null progress on 404 and auth on 401`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(404, "{}")
            assertEquals(
                DomainResult.Success(null),
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                ),
            )

            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.fetchProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "stale",
                    audiobookId = "item1",
                ),
            )
        }

    @Test
    fun `saveProgress posts derived ratio and honors offline`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, "{}")

            val result =
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(150.0, 300.0, false),
                )

            assertEquals(DomainResult.Success(Unit), result)
            val body = JSONObject(serverRule.takeRequest().body.readUtf8())
            assertEquals(150.0, body.getDouble("currentTime"), 0.0)
            assertEquals(300.0, body.getDouble("duration"), 0.0)
            assertEquals(0.5, body.getDouble("progress"), 0.0)
            assertEquals(false, body.getBoolean("isFinished"))

            connectivity.setConnected(false)
            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.saveProgress(
                    baseUrl = serverRule.baseUrl(),
                    accessToken = "token",
                    audiobookId = "item1",
                    progress = PlaybackProgress(1.0, 2.0, false),
                ),
            )
        }

    private fun cachedBook(): AudiobookEntity =
        AudiobookEntity(
            id = "cached",
            libraryId = "lib1",
            title = "Cached",
            author = "Author",
            coverUrl = "https://host/cover",
            series = null,
            durationInSeconds = 10L,
        )

    private fun bookJson(
        id: String,
        libraryId: String = "lib1",
        mediaType: String = "book",
    ): String =
        """{"id": "$id", "libraryId": "$libraryId", "mediaType": "$mediaType",
            "media": {"duration": 7265.0,
              "metadata": {"title": "Dune", "authorName": "Herbert, Frank"}}}"""

    private fun fullBookJson(): String =
        """{"id": "item1", "libraryId": "lib1", "mediaType": "book",
            "media": {"duration": 200.0,
              "metadata": {"title": "Dune", "authorName": "Herbert, Frank",
                "description": "Desert epic."},
              "chapters": [
                {"title": "One", "start": 0.0, "end": 100.0},
                {"title": "Two", "start": 100.0, "end": 200.0}
              ],
              "tracks": [
                {"index": 0, "startOffset": 0.0, "duration": 100.0,
                 "title": "Part 1", "contentUrl": "/audio/1.mp3", "mimeType": "audio/mpeg"},
                {"index": 1, "startOffset": 100.0, "duration": 100.0,
                 "title": "Part 2", "contentUrl": "/audio/2.mp3", "mimeType": "audio/mpeg"}
              ]}}"""
}
