package org.evoionosp.noveliq.domain.library.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

class SelectLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(libraryId: String): DomainResult<Unit> {
        return libraryRepository.selectLibrary(libraryId)
    }
}
