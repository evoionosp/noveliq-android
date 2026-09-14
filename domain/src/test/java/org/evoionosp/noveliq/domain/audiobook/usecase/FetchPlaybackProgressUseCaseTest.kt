package org.evoionosp.noveliq.domain.audiobook.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.testing.FakeAudiobookRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FetchPlaybackProgressUseCaseTest {
    @Test
    fun `returns recorded progress for resume`() =
        runTest {
            val progress = PlaybackProgress(currentTimeSeconds = 120.0, durationSeconds = 300.0, isFinished = false)
            val useCase =
                FetchPlaybackProgressUseCase(
                    FakeAudiobookRepository(fetchProgressResult = DomainResult.Success(progress)),
                )

            assertEquals(progress, useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `returns null when the server has no progress recorded`() =
        runTest {
            val useCase =
                FetchPlaybackProgressUseCase(
                    FakeAudiobookRepository(fetchProgressResult = DomainResult.Success(null)),
                )

            assertNull(useCase("https://example.com", "token", "book-1"))
        }

    @Test
    fun `returns null instead of failing when the fetch errors`() =
        runTest {
            // Playback must still start from the beginning when progress is
            // unavailable; the caller treats null as "no resume point".
            val useCase =
                FetchPlaybackProgressUseCase(
                    FakeAudiobookRepository(
                        fetchProgressResult = DomainResult.Failure(CatalogError.NETWORK),
                    ),
                )

            assertNull(useCase("https://example.com", "token", "book-1"))
        }
}
