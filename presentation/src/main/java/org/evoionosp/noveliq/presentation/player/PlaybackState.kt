package org.evoionosp.noveliq.presentation.player

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook

data class PlaybackState(
    val audiobook: Audiobook? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    // Absolute position across the whole book (seconds), used to resolve the current chapter.
    val currentBookPositionSeconds: Double = 0.0
)
