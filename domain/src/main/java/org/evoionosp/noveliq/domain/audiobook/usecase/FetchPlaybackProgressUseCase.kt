package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.DomainResult

/**
 * Fetches the server-side listening progress for an audiobook, or null when none is recorded or the
 * request fails. Callers use [PlaybackProgress.resumeSeconds] to decide where to resume.
 */
class FetchPlaybackProgressUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): PlaybackProgress? {
        return when (val result = audiobookRepository.fetchProgress(baseUrl, accessToken, audiobookId)) {
            is DomainResult.Success -> result.data
            is DomainResult.Failure -> null
        }
    }
}
