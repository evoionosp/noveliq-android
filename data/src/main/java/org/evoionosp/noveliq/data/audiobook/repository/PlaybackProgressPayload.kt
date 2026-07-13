package org.evoionosp.noveliq.data.audiobook.repository

import org.evoionosp.noveliq.data.library.remote.dto.UpdateProgressRequestDto
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress

/**
 * Builds the request body for `PATCH /api/me/progress/{itemId}`.
 *
 * Returns `null` when the request should NOT be sent because the required data is
 * missing. Callers must skip the HTTP call in that case: sending a synthetic body
 * when we cannot compute a real progress ratio would overwrite the server's actual
 * progress for this audiobook.
 */
internal fun buildUpdateProgressRequestBody(
    progress: PlaybackProgress,
): UpdateProgressRequestDto? {
    val duration = progress.durationSeconds?.takeIf { it > 0 }
    val ratio = if (duration != null) {
        (progress.currentTimeSeconds / duration).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    return UpdateProgressRequestDto(
        currentTime = progress.currentTimeSeconds,
        duration = duration,
        progress = ratio,
        isFinished = progress.isFinished,
    )
}
