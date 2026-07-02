package org.evoionosp.noveliq.domain.audiobook.model

/**
 * A user's listening progress for an audiobook, expressed as an absolute position across the whole
 * book (not per-track). This mirrors the Audiobookshelf `/api/me/progress` media progress model.
 */
data class PlaybackProgress(
    val currentTimeSeconds: Double,
    val durationSeconds: Double?,
    val isFinished: Boolean
)
