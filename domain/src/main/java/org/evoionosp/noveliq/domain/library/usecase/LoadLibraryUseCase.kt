package org.evoionosp.noveliq.domain.library.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

class LoadLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String
    ): Boolean {
        return repository.loadLibrary(baseUrl, accessToken)
    }
}
