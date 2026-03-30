package org.evoionosp.noveliq.domain.library.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.BootstrapHomeCatalogResult
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

class BootstrapHomeCatalogUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val audiobookRepository: AudiobookRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String
    ): BootstrapHomeCatalogResult {
        val libraryRefreshResult = libraryRepository.refreshLibraries(
            baseUrl = baseUrl,
            accessToken = accessToken
        )

        val libraries = libraryRepository.observeLibraries().first()
        if (libraries.isEmpty()) {
            return when (libraryRefreshResult) {
                is DomainResult.Failure -> {
                    if (libraryRefreshResult.error == CatalogError.NO_AUDIOBOOK_LIBRARIES) {
                        BootstrapHomeCatalogResult.NoLibrariesAvailable
                    } else {
                        BootstrapHomeCatalogResult.Failure(libraryRefreshResult.error)
                    }
                }
                is DomainResult.Success -> BootstrapHomeCatalogResult.NoLibrariesAvailable
            }
        }

        val selectedLibrary = libraryRepository.observeSelectedLibrary().first()
            ?: libraries.first()

        if (!selectedLibrary.isSelected) {
            val selectionResult = libraryRepository.selectLibrary(selectedLibrary.id)
            if (selectionResult is DomainResult.Failure) {
                return BootstrapHomeCatalogResult.Failure(selectionResult.error)
            }
        }

        val audiobookRefreshResult = audiobookRepository.refreshAudiobooks(
            baseUrl = baseUrl,
            accessToken = accessToken,
            libraryId = selectedLibrary.id
        )

        val cachedAudiobooks = audiobookRepository.observeAudiobooks(selectedLibrary.id).first()
        return when (audiobookRefreshResult) {
            is DomainResult.Success -> BootstrapHomeCatalogResult.Success(
                selectedLibraryId = selectedLibrary.id,
                usedCachedData = false
            )
            is DomainResult.Failure -> {
                if (cachedAudiobooks.isNotEmpty()) {
                    BootstrapHomeCatalogResult.Success(
                        selectedLibraryId = selectedLibrary.id,
                        usedCachedData = true
                    )
                } else {
                    BootstrapHomeCatalogResult.Failure(audiobookRefreshResult.error)
                }
            }
        }
    }
}
