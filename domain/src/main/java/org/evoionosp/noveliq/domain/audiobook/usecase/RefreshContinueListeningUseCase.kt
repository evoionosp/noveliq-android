package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.DomainResult

class RefreshContinueListeningUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> {
        return audiobookRepository.refreshContinueListening(baseUrl, accessToken, libraryId)
    }
}
