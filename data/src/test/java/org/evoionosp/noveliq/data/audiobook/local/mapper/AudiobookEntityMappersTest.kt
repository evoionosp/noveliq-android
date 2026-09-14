package org.evoionosp.noveliq.data.audiobook.local.mapper

import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookTrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudiobookEntityMappersTest {
    @Test
    fun `entity toDomain preserves summary fields`() {
        val audiobook =
            AudiobookEntity(
                id = "item1",
                libraryId = "lib1",
                title = "Dune",
                author = "Frank Herbert",
                coverUrl = "https://host/cover",
                series = "Dune Saga",
                durationInSeconds = 7265L,
            ).toDomain()

        assertEquals("item1", audiobook.id)
        assertEquals("lib1", audiobook.libraryId)
        assertEquals("Dune", audiobook.title)
        assertEquals("Frank Herbert", audiobook.author)
        assertEquals("https://host/cover", audiobook.coverUrl)
        assertEquals("Dune Saga", audiobook.series)
        assertEquals(7265L, audiobook.durationInSeconds)
    }

    @Test
    fun `detail toDomain maps book chapters tracks and refresh time`() {
        val detail =
            AudiobookDetailEntity(
                audiobookId = "item1",
                libraryId = "lib1",
                title = "Dune",
                author = "Frank Herbert",
                coverUrl = "https://host/cover",
                series = null,
                durationInSeconds = 200L,
                description = "Desert epic.",
                refreshedAtMillis = 99L,
            ).toDomain(
                chapters =
                    listOf(
                        AudiobookChapterEntity(
                            audiobookId = "item1",
                            chapterIndex = 0,
                            title = "One",
                            startInSeconds = 0L,
                            endInSeconds = 100L,
                        ),
                    ),
                tracks =
                    listOf(
                        AudiobookTrackEntity(
                            audiobookId = "item1",
                            trackIndex = 0,
                            startOffsetInSeconds = 0L,
                            durationInSeconds = 200L,
                            title = "Part 1",
                            remoteUrl = "https://host/audio.mp3",
                            mimeType = "audio/mpeg",
                        ),
                    ),
            )

        assertEquals("item1", detail.audiobook.id)
        assertEquals("Dune", detail.audiobook.title)
        assertNull(detail.audiobook.series)
        assertEquals("Desert epic.", detail.description)
        assertEquals(99L, detail.refreshedAtMillis)
        assertEquals("One", detail.chapters.single().title)
        assertEquals(100L, detail.chapters.single().endInSeconds)
        assertEquals("https://host/audio.mp3", detail.tracks.single().remoteUrl)
        assertEquals("audio/mpeg", detail.tracks.single().mimeType)
    }
}
