package org.evoionosp.noveliq.presentation.player

import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.junit.Assert.assertEquals
import org.junit.Test

class InProgressChapterIndexTest {
    private fun chapter(
        title: String,
        startInSeconds: Long,
    ) = AudiobookChapter(
        title = title,
        startInSeconds = startInSeconds,
        endInSeconds = null,
    )

    @Test
    fun `returns the last chapter at or before the position`() {
        val chapters =
            listOf(
                chapter("One", 0),
                chapter("Two", 600),
                chapter("Three", 1200),
            )

        assertEquals(0, inProgressChapterIndex(chapters, 0.0))
        assertEquals(0, inProgressChapterIndex(chapters, 599.9))
        assertEquals(1, inProgressChapterIndex(chapters, 600.0))
        assertEquals(2, inProgressChapterIndex(chapters, 5000.0))
    }

    @Test
    fun `returns minus one without chapters or before the first starts`() {
        assertEquals(-1, inProgressChapterIndex(emptyList(), 100.0))
        assertEquals(
            -1,
            inProgressChapterIndex(listOf(chapter("One", 300)), 299.9),
        )
    }
}
