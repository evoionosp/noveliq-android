package org.evoionosp.noveliq.domain.audiobook.playback

/**
 * A playback position expressed in terms of the underlying media items: which track
 * ([trackIndex], its position in the ordered track list) and how far into it ([offsetMs]).
 */
data class TrackPosition(
    val trackIndex: Int,
    val offsetMs: Long
)
