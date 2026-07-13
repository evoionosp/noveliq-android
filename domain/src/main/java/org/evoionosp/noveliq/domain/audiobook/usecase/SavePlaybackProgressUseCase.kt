package org.evoionosp.noveliq.domain.audiobook.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.DomainResult

/**
 * Persists listening progress for an audiobook, deriving the "finished" flag from the current
 * position and total duration so that completion policy lives in the domain, not the player.
 */
class SavePlaybackProgressUseCase @Inject constructor(
    private val audiobookRepository: AudiobookRepository,
    private val calculator: PlaybackPositionCalculator
) {
    suspend operator fun invoke(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        absoluteSeconds: Double,
        totalSeconds: Double?
    ): DomainResult<Unit> {
        return audiobookRepository.saveProgress(
            baseUrl = baseUrl,
            accessToken = accessToken,
            audiobookId = audiobookId,
            progress = PlaybackProgress(
                currentTimeSeconds = absoluteSeconds,
                durationSeconds = totalSeconds,
                isFinished = calculator.isFinished(absoluteSeconds, totalSeconds)
            )
        )
    }
}
