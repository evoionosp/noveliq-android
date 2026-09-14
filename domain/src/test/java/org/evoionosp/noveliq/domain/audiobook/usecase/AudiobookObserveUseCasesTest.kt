package org.evoionosp.noveliq.domain.audiobook.usecase

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.domain.testing.FakeAudiobookRepository
import org.evoionosp.noveliq.domain.testing.testAudiobook
import org.evoionosp.noveliq.domain.testing.testDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class AudiobookObserveUseCasesTest {
    @Test
    fun `observe use cases emit the repository state`() =
        runTest {
            val repository = FakeAudiobookRepository()
            val books = listOf(testAudiobook(), testAudiobook(id = "book-2"))
            repository.setAudiobooks("lib-1", books)
            repository.continueListening = listOf(testAudiobook(id = "book-3"))
            repository.setDetail("lib-1", "book-1", testDetail())
            repository.syncStatus = SyncStatus.Success(lastSyncedAtMillis = 42L)

            ObserveHomeAudiobooksUseCase(repository)("lib-1").test {
                assertEquals(books, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            ObserveContinueListeningUseCase(repository)("lib-1").test {
                assertEquals(listOf(testAudiobook(id = "book-3")), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            ObserveAudiobookDetailUseCase(repository)("lib-1", "book-1").test {
                assertEquals(testDetail(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            ObserveLibrarySyncStatusUseCase(repository)("lib-1").test {
                assertEquals(SyncStatus.Success(42L), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh use cases delegate results to the repository`() =
        runTest {
            val repository =
                FakeAudiobookRepository(
                    refreshDetailResult = DomainResult.Failure(CatalogError.NOT_FOUND),
                    refreshBooksResult = DomainResult.Failure(CatalogError.NETWORK),
                    refreshContinueResult = DomainResult.Success(Unit),
                )

            assertEquals(
                DomainResult.Failure(CatalogError.NOT_FOUND),
                RefreshAudiobookDetailUseCase(repository)("https://example.com", "token", "lib-1", "book-1"),
            )
            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                RefreshSelectedLibraryAudiobooksUseCase(repository)("https://example.com", "token", "lib-1"),
            )
            assertEquals(
                DomainResult.Success(Unit),
                RefreshContinueListeningUseCase(repository)("https://example.com", "token", "lib-1"),
            )
        }
}
