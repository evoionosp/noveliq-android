package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository

class ObserveAudiobookDetailUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    operator fun invoke(libraryId: String, audiobookId: String): Flow<AudiobookDetail?> {
        return audiobookRepository.observeAudiobookDetail(libraryId, audiobookId)
    }
}
