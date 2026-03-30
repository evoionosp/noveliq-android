package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.SyncStatus

class ObserveLibrarySyncStatusUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    operator fun invoke(libraryId: String): Flow<SyncStatus> {
        return audiobookRepository.observeLibrarySyncStatus(libraryId)
    }
}
