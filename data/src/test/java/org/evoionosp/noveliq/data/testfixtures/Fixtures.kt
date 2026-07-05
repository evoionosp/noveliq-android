package org.evoionosp.noveliq.data.testfixtures

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack

/**
 * Test fixture factories. Pre-seeded on `download-v1` for consistent, minimal test setup.
 *
 * Every factory takes named defaults so tests can override only the fields they care about:
 * ```
 * val book = Fixtures.audiobook(id = "book-42", title = "Custom")
 * val track = Fixtures.track(remoteUrl = "https://server/audio.mp3")
 * ```
 */
object Fixtures {
    fun audiobook(
        id: String = "book-1",
        libraryId: String = "lib-1",
        title: String = "Test Audiobook",
        author: String = "Test Author",
        coverUrl: String = "",
        series: String? = null,
        durationInSeconds: Long? = 3600L,
    ): Audiobook =
        Audiobook(
            id = id,
            libraryId = libraryId,
            title = title,
            author = author,
            coverUrl = coverUrl,
            series = series,
            durationInSeconds = durationInSeconds,
        )

    fun track(
        index: Int = 0,
        title: String = "Track ${index + 1}",
        remoteUrl: String = "https://example.test/audio/$index.mp3",
        startOffsetInSeconds: Long = 0L,
        durationInSeconds: Long = 60L,
        mimeType: String? = "audio/mpeg",
    ): AudiobookTrack =
        AudiobookTrack(
            index = index,
            title = title,
            remoteUrl = remoteUrl,
            startOffsetInSeconds = startOffsetInSeconds,
            durationInSeconds = durationInSeconds,
            mimeType = mimeType,
        )

    fun chapter(
        title: String = "Chapter 1",
        startInSeconds: Long = 0L,
        endInSeconds: Long? = 60L,
    ): AudiobookChapter =
        AudiobookChapter(
            title = title,
            startInSeconds = startInSeconds,
            endInSeconds = endInSeconds,
        )

    fun detail(
        audiobook: Audiobook = audiobook(),
        description: String? = null,
        chapters: List<AudiobookChapter> = listOf(chapter()),
        tracks: List<AudiobookTrack> = listOf(track()),
        refreshedAtMillis: Long = 0L,
    ): AudiobookDetail =
        AudiobookDetail(
            audiobook = audiobook,
            description = description,
            chapters = chapters,
            tracks = tracks,
            refreshedAtMillis = refreshedAtMillis,
        )
}
