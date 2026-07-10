package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository

/**
 * Ensures an audiobook's detail (including playable tracks) is available before playback, refreshing
 * from the server on a cache miss. Returns the detail only when it actually has tracks, or null when
 * the book cannot be played.
 */
class PreparePlaybackUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String
    ): AudiobookDetail? {
        var detail = audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
        if (detail == null || detail.tracks.isEmpty()) {
            audiobookRepository.refreshAudiobookDetail(
                baseUrl = baseUrl,
                accessToken = accessToken,
                libraryId = libraryId,
                audiobookId = audiobookId
            )
            detail = audiobookRepository.observeAudiobookDetail(libraryId, audiobookId).first()
        }
        return detail?.takeIf { it.tracks.isNotEmpty() }
    }
}
