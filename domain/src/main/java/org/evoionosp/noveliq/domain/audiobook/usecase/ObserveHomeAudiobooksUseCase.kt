package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository

class ObserveHomeAudiobooksUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    operator fun invoke(libraryId: String): Flow<List<Audiobook>> {
        return audiobookRepository.observeAudiobooks(libraryId)
    }
}
