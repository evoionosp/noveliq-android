package org.evoionosp.noveliq.data.library.remote.mapper

import org.evoionosp.noveliq.data.library.remote.dto.AudioTrackDto
import org.evoionosp.noveliq.data.library.remote.dto.ChapterDto
import org.evoionosp.noveliq.data.library.remote.dto.LibraryDto
import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemDto
import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemMediaDto
import org.evoionosp.noveliq.data.library.remote.dto.LibraryItemMetadataDto
import org.evoionosp.noveliq.data.library.remote.dto.SeriesDto
import org.evoionosp.noveliq.data.library.remote.dto.UserMediaProgressDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRemoteMappersTest {
    @Test
    fun `library toEntity maps fields and selection`() {
        val entity =
            LibraryDto(
                id = "lib1",
                name = "Audiobooks",
                displayOrder = 2,
                mediaType = "book",
            ).toEntity(isSelected = true)!!

        assertEquals("lib1", entity.id)
        assertEquals("Audiobooks", entity.name)
        assertEquals(2, entity.displayOrder)
        assertTrue(entity.isSelected)
    }

    @Test
    fun `library toEntity returns null for blank id and defaults name and order`() {
        assertNull(LibraryDto(id = "  ", name = "X").toEntity(isSelected = false))

        val entity =
            LibraryDto(
                id = "lib1",
                name = "  ",
                displayOrder = null,
                mediaType = "book",
            ).toEntity(isSelected = false)!!

        assertEquals("Library", entity.name)
        assertEquals(Int.MAX_VALUE, entity.displayOrder)
        assertFalse(entity.isSelected)
    }

    @Test
    fun `isAudiobookLibrary matches book case-insensitively`() {
        assertTrue(LibraryDto(mediaType = "book").isAudiobookLibrary())
        assertTrue(LibraryDto(mediaType = "BOOK").isAudiobookLibrary())
        assertFalse(LibraryDto(mediaType = "podcast").isAudiobookLibrary())
        assertFalse(LibraryDto(mediaType = null).isAudiobookLibrary())
    }

    @Test
    fun `item toEntity maps summary with cover url and flipped author name`() {
        val entity =
            bookItem(
                metadata =
                    LibraryItemMetadataDto(
                        title = "The Fellowship",
                        authorName = "Tolkien, J.R.R.",
                        seriesName = "LOTR",
                    ),
            ).toEntity(baseUrl = "https://host", fallbackLibraryId = "lib1")!!

        assertEquals("item1", entity.id)
        assertEquals("lib1", entity.libraryId)
        assertEquals("The Fellowship", entity.title)
        assertEquals("J.R.R. Tolkien", entity.author)
        assertEquals(
            "https://host/api/items/item1/cover?width=400&format=webp",
            entity.coverUrl,
        )
        assertEquals("LOTR", entity.series)
        assertEquals(7265L, entity.durationInSeconds)
    }

    @Test
    fun `item toEntity prefers explicit library id and series list fallback`() {
        val entity =
            bookItem(
                libraryId = "lib9",
                metadata =
                    LibraryItemMetadataDto(
                        title = "T",
                        authorName = "A",
                        series = listOf(SeriesDto(name = "Saga")),
                    ),
            ).toEntity(baseUrl = "https://host/", fallbackLibraryId = "lib1")!!

        assertEquals("lib9", entity.libraryId)
        assertEquals("Saga", entity.series)
    }

    @Test
    fun `item toEntity defaults blank metadata and rejects non-books`() {
        val entity =
            LibraryItemDto(
                id = "item1",
                libraryId = null,
                mediaType = "book",
                media = LibraryItemMediaDto(),
            ).toEntity(baseUrl = "https://host", fallbackLibraryId = "lib1")!!

        assertEquals("Untitled", entity.title)
        assertEquals("Unknown Author", entity.author)
        assertNull(entity.series)
        assertNull(entity.durationInSeconds)

        assertNull(
            bookItem()
                .copy(mediaType = "podcast")
                .toEntity(baseUrl = "https://host", fallbackLibraryId = "lib1"),
        )
        assertNull(
            bookItem()
                .copy(id = " ")
                .toEntity(baseUrl = "https://host", fallbackLibraryId = "lib1"),
        )
    }

    @Test
    fun `item toEntity leaves single names unflipped`() {
        val entity =
            bookItem(
                metadata = LibraryItemMetadataDto(title = "Meditations", authorName = "Plato"),
            ).toEntity(baseUrl = "https://host", fallbackLibraryId = "lib1")!!

        assertEquals("Plato", entity.author)
    }

    @Test
    fun `item toDetailEntity carries description and refresh time`() {
        val detail =
            bookItem(
                metadata =
                    LibraryItemMetadataDto(
                        title = "Dune",
                        authorName = "Herbert, Frank",
                        description = "Desert epic.",
                    ),
            ).toDetailEntity(
                baseUrl = "https://host",
                fallbackLibraryId = "lib1",
                refreshedAtMillis = 123L,
            )!!

        assertEquals("item1", detail.audiobookId)
        assertEquals("Dune", detail.title)
        assertEquals("Frank Herbert", detail.author)
        assertEquals("Desert epic.", detail.description)
        assertEquals(123L, detail.refreshedAtMillis)
    }

    @Test
    fun `item toDetailEntity drops blank descriptions and nulls with bad summary`() {
        val detail =
            bookItem(
                metadata = LibraryItemMetadataDto(title = "Dune", description = "  "),
            ).toDetailEntity(
                baseUrl = "https://host",
                fallbackLibraryId = "lib1",
                refreshedAtMillis = 1L,
            )!!

        assertNull(detail.description)
        assertNull(
            bookItem().copy(mediaType = "podcast").toDetailEntity(
                baseUrl = "https://host",
                fallbackLibraryId = "lib1",
                refreshedAtMillis = 1L,
            ),
        )
    }

    @Test
    fun `item toChapterEntities sorts by start and reindexes with title fallback`() {
        val chapters =
            bookItem(
                chapters =
                    listOf(
                        ChapterDto(title = "Two", startInSeconds = 100f, endInSeconds = 200f),
                        ChapterDto(title = null, startInSeconds = 0f, endInSeconds = 100f),
                    ),
            ).toChapterEntities(audiobookId = "item1")

        assertEquals(2, chapters.size)
        assertEquals(0, chapters[0].chapterIndex)
        assertEquals("Chapter 2", chapters[0].title)
        assertEquals(0L, chapters[0].startInSeconds)
        assertEquals(1, chapters[1].chapterIndex)
        assertEquals("Two", chapters[1].title)
        assertEquals("item1", chapters[1].audiobookId)
    }

    @Test
    fun `item toChapterEntities defaults missing bounds`() {
        val chapters =
            bookItem(chapters = listOf(ChapterDto(title = "Only", startInSeconds = null)))
                .toChapterEntities(audiobookId = "item1")

        assertEquals(0L, chapters.single().startInSeconds)
        assertNull(chapters.single().endInSeconds)
    }

    @Test
    fun `item toTrackEntities drops url-less tracks and joins remote urls`() {
        val tracks =
            bookItem(
                tracks =
                    listOf(
                        AudioTrackDto(
                            index = null,
                            startOffsetInSeconds = 0f,
                            durationInSeconds = 100f,
                            title = null,
                            contentUrl = "audio/a.mp3",
                            mimeType = null,
                        ),
                        AudioTrackDto(title = "NoUrl", contentUrl = "  "),
                        AudioTrackDto(
                            index = 5,
                            startOffsetInSeconds = 100f,
                            durationInSeconds = 100f,
                            title = "B",
                            contentUrl = "/audio/b.mp3",
                            mimeType = "audio/mpeg",
                        ),
                    ),
            ).toTrackEntities(baseUrl = "https://host", audiobookId = "item1")

        assertEquals(2, tracks.size)
        assertEquals(0, tracks[0].trackIndex)
        assertEquals("Track 1", tracks[0].title)
        assertEquals("https://host/audio/a.mp3", tracks[0].remoteUrl)
        assertEquals(5, tracks[1].trackIndex)
        assertEquals("https://host/audio/b.mp3", tracks[1].remoteUrl)
        assertEquals("audio/mpeg", tracks[1].mimeType)
    }

    @Test
    fun `item toContinueListeningEntity falls back to library and zero progress`() {
        val entity =
            bookItem(libraryId = null, progressLastUpdateMillis = null)
                .toContinueListeningEntity(fallbackLibraryId = "lib1")!!

        assertEquals("item1", entity.audiobookId)
        assertEquals("lib1", entity.libraryId)
        assertEquals(0L, entity.progressLastUpdateMillis)
    }

    @Test
    fun `item toContinueListeningEntity maps the reported playback position`() {
        val entity =
            bookItem(userMediaProgress = UserMediaProgressDto(currentTime = 120.5))
                .toContinueListeningEntity(fallbackLibraryId = "lib1")!!

        assertEquals(120.5, entity.currentTimeSeconds!!, 0.001)
    }

    @Test
    fun `item toContinueListeningEntity leaves position null without progress`() {
        val entity =
            bookItem(userMediaProgress = null)
                .toContinueListeningEntity(fallbackLibraryId = "lib1")!!

        assertNull(entity.currentTimeSeconds)
    }

    @Test
    fun `item toContinueListeningEntity rejects blank ids and non-books`() {
        assertNull(
            bookItem().copy(id = null).toContinueListeningEntity(fallbackLibraryId = "lib1"),
        )
        assertNull(
            bookItem()
                .copy(mediaType = "podcast")
                .toContinueListeningEntity(fallbackLibraryId = "lib1"),
        )
    }

    private fun bookItem(
        libraryId: String? = "lib1",
        metadata: LibraryItemMetadataDto? =
            LibraryItemMetadataDto(
                title = "Title",
                authorName = "Author",
            ),
        chapters: List<ChapterDto>? = null,
        tracks: List<AudioTrackDto>? = null,
        progressLastUpdateMillis: Long? = 42L,
        userMediaProgress: UserMediaProgressDto? = null,
    ): LibraryItemDto =
        LibraryItemDto(
            id = "item1",
            libraryId = libraryId,
            mediaType = "book",
            media =
                LibraryItemMediaDto(
                    durationInSeconds = 7265f,
                    chapters = chapters,
                    tracks = tracks,
                    metadata = metadata,
                ),
            progressLastUpdateMillis = progressLastUpdateMillis,
            userMediaProgress = userMediaProgress,
        )
}
