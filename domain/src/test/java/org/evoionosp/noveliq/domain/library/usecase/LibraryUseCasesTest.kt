package org.evoionosp.noveliq.domain.library.usecase

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.testing.FakeLibraryRepository
import org.evoionosp.noveliq.domain.testing.testLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryUseCasesTest {
    @Test
    fun `observe use cases emit the repository state`() =
        runTest {
            val selected = testLibrary(isSelected = true)
            val repository =
                FakeLibraryRepository(
                    libraries = listOf(selected, testLibrary(id = "lib-2")),
                    selectedLibrary = selected,
                )

            ObserveLibrariesUseCase(repository)().test {
                assertEquals(2, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
            ObserveSelectedLibraryUseCase(repository)().test {
                assertEquals(selected, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh libraries delegates the repository result`() =
        runTest {
            val repository =
                FakeLibraryRepository(
                    refreshResult = DomainResult.Failure(CatalogError.NETWORK),
                )

            assertEquals(
                DomainResult.Failure(CatalogError.NETWORK),
                RefreshLibrariesUseCase(repository)("https://example.com", "token"),
            )
            assertEquals(1, repository.refreshCallCount)
        }

    @Test
    fun `select library updates the selection`() =
        runTest {
            val repository =
                FakeLibraryRepository(
                    libraries = listOf(testLibrary(isSelected = true), testLibrary(id = "lib-2")),
                    selectedLibrary = testLibrary(isSelected = true),
                )

            val result = SelectLibraryUseCase(repository)("lib-2")

            assertEquals(DomainResult.Success(Unit), result)
            assertEquals("lib-2", repository.selectedLibraryFlow.value?.id)
        }

    @Test
    fun `select library passes an override failure through`() =
        runTest {
            val repository =
                FakeLibraryRepository(
                    libraries = listOf(testLibrary(isSelected = true)),
                    selectedLibrary = testLibrary(isSelected = true),
                    selectResultOverride = DomainResult.Failure(CatalogError.AUTH),
                )

            assertEquals(
                DomainResult.Failure(CatalogError.AUTH),
                SelectLibraryUseCase(repository)("lib-1"),
            )
        }
}
