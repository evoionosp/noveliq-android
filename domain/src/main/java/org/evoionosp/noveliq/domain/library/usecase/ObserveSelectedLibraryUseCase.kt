package org.evoionosp.noveliq.domain.library.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

class ObserveSelectedLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    operator fun invoke(): Flow<AudiobookLibrary?> = libraryRepository.observeSelectedLibrary()
}
