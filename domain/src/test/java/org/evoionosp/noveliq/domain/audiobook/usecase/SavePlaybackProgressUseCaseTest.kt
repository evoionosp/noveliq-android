package org.evoionosp.noveliq.domain.audiobook.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.testing.FakeAudiobookRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavePlaybackProgressUseCaseTest {
    @Test
    fun `marks progress finished near the end of the book`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val useCase = SavePlaybackProgressUseCase(repository, PlaybackPositionCalculator())

            val result = useCase("https://example.com", "token", "book-1", absoluteSeconds = 297.0, totalSeconds = 300.0)

            assertEquals(DomainResult.Success(Unit), result)
            assertEquals(1, repository.savedProgress.size)
            assertTrue(repository.savedProgress.single().isFinished)
            assertEquals(297.0, repository.savedProgress.single().currentTimeSeconds, 0.001)
            assertEquals(300.0, repository.savedProgress.single().durationSeconds!!, 0.001)
        }

    @Test
    fun `leaves progress unfinished mid-book`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val useCase = SavePlaybackProgressUseCase(repository, PlaybackPositionCalculator())

            useCase("https://example.com", "token", "book-1", absoluteSeconds = 120.0, totalSeconds = 300.0)

            assertFalse(repository.savedProgress.single().isFinished)
        }

    @Test
    fun `leaves progress unfinished when the total duration is unknown`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val useCase = SavePlaybackProgressUseCase(repository, PlaybackPositionCalculator())

            useCase("https://example.com", "token", "book-1", absoluteSeconds = 297.0, totalSeconds = null)

            assertFalse(repository.savedProgress.single().isFinished)
        }

    @Test
    fun `passes a save failure through to the caller`() =
        runTest {
            val repository =
                FakeAudiobookRepository(
                    saveProgressResult = DomainResult.Failure(CatalogError.NETWORK),
                )
            val useCase = SavePlaybackProgressUseCase(repository, PlaybackPositionCalculator())

            val result = useCase("https://example.com", "token", "book-1", absoluteSeconds = 10.0, totalSeconds = 300.0)

            assertEquals(DomainResult.Failure(CatalogError.NETWORK), result)
        }
}
