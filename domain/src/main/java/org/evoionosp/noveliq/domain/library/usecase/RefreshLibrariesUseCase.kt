package org.evoionosp.noveliq.domain.library.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

class RefreshLibrariesUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String
    ): DomainResult<Unit> {
        return libraryRepository.refreshLibraries(baseUrl, accessToken)
    }
}
