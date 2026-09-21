package org.evoionosp.noveliq.playback

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook

data class PlaybackState(
    val audiobook: Audiobook? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    // Absolute position across the whole book (seconds), used to resolve the current chapter.
    val currentBookPositionSeconds: Double = 0.0,
    val sleepTimer: SleepTimerState = SleepTimerState.Off,
    /**
     * Milliseconds left on an armed wall-clock timer, refreshed by the
     * progress loop. Null when no timer countdown is running. Carried in
     * state (rather than derived in the UI) so the countdown recomposes
     * every second even while paused, when nothing else changes.
     */
    val sleepRemainingMs: Long? = null,
)
