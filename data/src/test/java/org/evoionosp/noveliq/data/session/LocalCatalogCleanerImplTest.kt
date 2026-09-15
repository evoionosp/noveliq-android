package org.evoionosp.noveliq.data.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.data.test.TestDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalCatalogCleanerImplTest {
    private lateinit var database: NoveliqDatabase

    @Before
    fun setUp() {
        database = TestDatabase.create()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `clear drops every catalog table`() =
        runTest {
            database.libraryDao().upsertLibraries(
                listOf(LibraryEntity(id = "lib-1", name = "Books", displayOrder = 0, isSelected = true)),
            )
            database.audiobookDao().upsertAudiobooks(
                listOf(
                    AudiobookEntity(
                        id = "book-1",
                        libraryId = "lib-1",
                        title = "Dune",
                        author = "Frank Herbert",
                        coverUrl = "https://example.com/cover.jpg",
                        series = null,
                        durationInSeconds = 3600L,
                    ),
                ),
            )
            database.continueListeningDao().upsert(
                listOf(ContinueListeningEntity(audiobookId = "book-1", libraryId = "lib-1", progressLastUpdateMillis = 42L)),
            )

            LocalCatalogCleanerImpl(database, Dispatchers.Unconfined).clear()

            assertEquals(0, database.libraryDao().countLibraries())
            assertEquals(0, database.audiobookDao().countByLibraryId("lib-1"))
            assertEquals(
                0,
                database
                    .continueListeningDao()
                    .observeContinueListening("lib-1")
                    .first()
                    .size,
            )
        }
}
