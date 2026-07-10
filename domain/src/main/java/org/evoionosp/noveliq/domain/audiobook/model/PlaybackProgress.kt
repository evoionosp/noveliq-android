package org.evoionosp.noveliq.domain.audiobook.model

/**
 * A user's listening progress for an audiobook, expressed as an absolute position across the whole
 * book (not per-track). This mirrors the Audiobookshelf `/api/me/progress` media progress model.
 */
data class PlaybackProgress(
    val currentTimeSeconds: Double,
    val durationSeconds: Double?,
    val isFinished: Boolean
) {
    /**
     * The absolute second to resume from, or null when there is nothing useful to resume to
     * (the book is finished or progress is at the very start).
     */
    val resumeSeconds: Double?
        get() = currentTimeSeconds.takeIf { !isFinished && it > 0 }
}
