package org.evoionosp.noveliq.presentation.common.model

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiModelsTest {
    @Test
    fun `time left shows hours and minutes`() {
        assertEquals("5h 30m left", timeLeftLabel(durationInSeconds = 36_000L, progressSeconds = 16_200.0))
    }

    @Test
    fun `time left pads minutes under an hour boundary`() {
        assertEquals("5h 00m left", timeLeftLabel(durationInSeconds = 36_000L, progressSeconds = 18_000.0))
    }

    @Test
    fun `time left shows minutes only under an hour`() {
        assertEquals("45m left", timeLeftLabel(durationInSeconds = 3_600L, progressSeconds = 900.0))
    }

    @Test
    fun `time left rounds to the nearest minute with a one minute floor`() {
        assertEquals("1h 59m left", timeLeftLabel(durationInSeconds = 7_265L, progressSeconds = 120.5))
        assertEquals("1m left", timeLeftLabel(durationInSeconds = 3_600L, progressSeconds = 3_580.0))
    }

    @Test
    fun `time left is null without duration progress or remaining time`() {
        assertNull(timeLeftLabel(durationInSeconds = null, progressSeconds = 120.0))
        assertNull(timeLeftLabel(durationInSeconds = 3_600L, progressSeconds = null))
        assertNull(timeLeftLabel(durationInSeconds = 3_600L, progressSeconds = 3_600.0))
        assertNull(timeLeftLabel(durationInSeconds = 3_600L, progressSeconds = 9_999.0))
    }

    @Test
    fun `toUiModel carries the time left label when progress is known`() {
        val uiModel = audiobook(progressSeconds = 16_200.0).toUiModel()

        assertEquals("5h 30m left", uiModel.timeLeftLabel)
    }

    @Test
    fun `toUiModel leaves the time left label null without progress`() {
        val uiModel = audiobook(progressSeconds = null).toUiModel()

        assertNull(uiModel.timeLeftLabel)
    }

    private fun audiobook(progressSeconds: Double?) =
        Audiobook(
            id = "book-1",
            libraryId = "lib-1",
            title = "Dune",
            author = "Frank Herbert",
            coverUrl = "https://example.com/cover.jpg",
            series = null,
            durationInSeconds = 36_000L,
            progressSeconds = progressSeconds,
        )
}
