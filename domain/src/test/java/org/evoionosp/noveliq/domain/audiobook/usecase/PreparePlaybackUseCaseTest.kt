package org.evoionosp.noveliq.domain.audiobook.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.testing.FakeAudiobookRepository
import org.evoionosp.noveliq.domain.testing.testAudiobook
import org.evoionosp.noveliq.domain.testing.testDetail
import org.evoionosp.noveliq.domain.testing.testTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreparePlaybackUseCaseTest {
    private val audiobook = testAudiobook()

    @Test
    fun `returns cached detail without refreshing when tracks are present`() =
        runTest {
            val repository = FakeAudiobookRepository()
            repository.setDetail("lib-1", "book-1", testDetail(audiobook))
            val useCase = PreparePlaybackUseCase(repository)

            val result = useCase("https://example.com", "token", "lib-1", "book-1")

            assertEquals(testDetail(audiobook), result)
            assertEquals(emptyList<Pair<String, String>>(), repository.refreshedDetails)
        }

    @Test
    fun `refreshes on a cache miss and returns the newly cached detail`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val fresh = testDetail(tracks = listOf(testTrack(), testTrack(index = 1, startOffsetInSeconds = 300)))
            repository.onRefreshDetail = { libraryId, audiobookId ->
                repository.setDetail(libraryId, audiobookId, fresh)
            }
            val useCase = PreparePlaybackUseCase(repository)

            val result = useCase("https://example.com", "token", "lib-1", "book-1")

            assertEquals(fresh, result)
            assertEquals(listOf("lib-1" to "book-1"), repository.refreshedDetails)
        }

    @Test
    fun `refreshes when the cached detail has no tracks`() =
        runTest {
            val repository = FakeAudiobookRepository()
            repository.setDetail("lib-1", "book-1", testDetail(tracks = emptyList()))
            val fresh = testDetail()
            repository.onRefreshDetail = { libraryId, audiobookId ->
                repository.setDetail(libraryId, audiobookId, fresh)
            }
            val useCase = PreparePlaybackUseCase(repository)

            val result = useCase("https://example.com", "token", "lib-1", "book-1")

            assertEquals(fresh, result)
            assertEquals(listOf("lib-1" to "book-1"), repository.refreshedDetails)
        }

    @Test
    fun `returns null when the refresh still leaves no playable tracks`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val useCase = PreparePlaybackUseCase(repository)

            assertNull(useCase("https://example.com", "token", "lib-1", "book-1"))
        }

    @Test
    fun `returns null when the refresh fails and nothing is cached`() =
        runTest {
            val repository =
                FakeAudiobookRepository(
                    refreshDetailResult = DomainResult.Failure(CatalogError.NETWORK),
                )
            val useCase = PreparePlaybackUseCase(repository)

            assertNull(useCase("https://example.com", "token", "lib-1", "book-1"))
        }
}
