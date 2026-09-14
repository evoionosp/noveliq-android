package org.evoionosp.noveliq.domain.audiobook.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressTest {
    @Test
    fun `resumeSeconds returns the position mid-book`() {
        val progress = PlaybackProgress(currentTimeSeconds = 123.5, durationSeconds = 300.0, isFinished = false)

        assertEquals(123.5, progress.resumeSeconds!!, 0.001)
    }

    @Test
    fun `resumeSeconds is null at the very start`() {
        val progress = PlaybackProgress(currentTimeSeconds = 0.0, durationSeconds = 300.0, isFinished = false)

        assertNull(progress.resumeSeconds)
    }

    @Test
    fun `resumeSeconds is null when the book is finished`() {
        val progress = PlaybackProgress(currentTimeSeconds = 299.0, durationSeconds = 300.0, isFinished = true)

        assertNull(progress.resumeSeconds)
    }
}
