package org.evoionosp.noveliq.data.library.repository

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.test.FakeConnectivityObserver
import org.evoionosp.noveliq.data.test.MockWebServerRule
import org.evoionosp.noveliq.data.test.TestDatabase
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val testDispatcher = StandardTestDispatcher()
    private val connectivity = FakeConnectivityObserver()

    private lateinit var database: NoveliqDatabase
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        database = TestDatabase.create()
        repository =
            LibraryRepositoryImpl(
                database = database,
                libraryDao = database.libraryDao(),
                audiobookDao = database.audiobookDao(),
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
    fun `refreshLibraries offline succeeds from cache and fails without`() =
        runTest(testDispatcher) {
            connectivity.setConnected(false)

            assertEquals(
                DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE),
                repository.refreshLibraries(serverRule.baseUrl(), "token"),
            )

            database.libraryDao().upsertLibraries(listOf(library("lib1", isSelected = true)))
            assertEquals(
                DomainResult.Success(Unit),
                repository.refreshLibraries(serverRule.baseUrl(), "token"),
            )
        }

    @Test
    fun `refreshLibraries keeps only audiobook libraries and selects the first`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"libraries": [
                    {"id": "lib1", "name": "Audiobooks", "displayOrder": 1, "mediaType": "book"},
                    {"id": "lib2", "name": "Pods", "displayOrder": 2, "mediaType": "podcast"}
                ]}""",
            )

            assertEquals(
                DomainResult.Success(Unit),
                repository.refreshLibraries(serverRule.baseUrl(), "token"),
            )
            assertEquals(listOf("lib1"), database.libraryDao().getLibraries().map { it.id })
            assertEquals("lib1", database.libraryDao().getSelectedLibrary()?.id)
        }

    @Test
    fun `refreshLibraries preserves the previous selection and prunes stale data`() =
        runTest(testDispatcher) {
            database.libraryDao().upsertLibraries(
                listOf(library("lib1", isSelected = true), library("lib-old")),
            )
            database.audiobookDao().upsertAudiobooks(listOf(book("gone", "lib-old")))
            database.librarySyncStateDao().upsert(
                LibrarySyncStateEntity("lib-old", "SUCCESS", 1L, null),
            )
            serverRule.enqueueJson(
                200,
                """{"libraries": [
                    {"id": "lib1", "name": "Audiobooks", "displayOrder": 1, "mediaType": "book"},
                    {"id": "lib3", "name": "More Books", "displayOrder": 2, "mediaType": "book"}
                ]}""",
            )

            assertEquals(
                DomainResult.Success(Unit),
                repository.refreshLibraries(serverRule.baseUrl(), "token"),
            )
            assertEquals(listOf("lib1", "lib3"), database.libraryDao().getLibraries().map { it.id })
            assertEquals("lib1", database.libraryDao().getSelectedLibrary()?.id)
            assertEquals(0, database.audiobookDao().countByLibraryId("lib-old"))
            assertNull(database.librarySyncStateDao().getSyncState("lib-old"))
        }

    @Test
    fun `refreshLibraries reports no audiobook libraries and wipes catalog`() =
        runTest(testDispatcher) {
            database.libraryDao().upsertLibraries(listOf(library("lib1", isSelected = true)))
            serverRule.enqueueJson(
                200,
                """{"libraries": [
                    {"id": "lib2", "name": "Pods", "displayOrder": 1, "mediaType": "podcast"}
                ]}""",
            )

            assertEquals(
                DomainResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES),
                repository.refreshLibraries(serverRule.baseUrl(), "token"),
            )
            assertEquals(0, database.libraryDao().countLibraries())
        }

    @Test
    fun `refreshLibraries maps auth errors and prefers stale cache`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                repository.refreshLibraries(serverRule.baseUrl(), "stale"),
            )

            database.libraryDao().upsertLibraries(listOf(library("lib1", isSelected = true)))
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                DomainResult.Success(Unit),
                repository.refreshLibraries(serverRule.baseUrl(), "stale"),
            )
        }

    @Test
    fun `refreshLibraries maps network errors and prefers stale cache`() =
        runTest(testDispatcher) {
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()

            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                repository.refreshLibraries(deadUrl, "token"),
            )

            database.libraryDao().upsertLibraries(listOf(library("lib1", isSelected = true)))
            assertEquals(
                DomainResult.Success(Unit),
                repository.refreshLibraries(deadUrl, "token"),
            )
        }

    @Test
    fun `selectLibrary rejects unknown ids and flips selection`() =
        runTest(testDispatcher) {
            database.libraryDao().upsertLibraries(
                listOf(library("lib1", isSelected = true), library("lib2")),
            )

            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                repository.selectLibrary("nope"),
            )
            assertEquals(
                DomainResult.Success(Unit),
                repository.selectLibrary("lib2"),
            )
            assertEquals("lib2", database.libraryDao().getSelectedLibrary()?.id)
        }

    private fun library(
        id: String,
        isSelected: Boolean = false,
    ): LibraryEntity =
        LibraryEntity(
            id = id,
            name = id,
            displayOrder = 1,
            isSelected = isSelected,
        )

    private fun book(
        id: String,
        libraryId: String,
    ): AudiobookEntity =
        AudiobookEntity(
            id = id,
            libraryId = libraryId,
            title = id,
            author = "Author",
            coverUrl = "https://host/cover",
            series = null,
            durationInSeconds = 10L,
        )
}
