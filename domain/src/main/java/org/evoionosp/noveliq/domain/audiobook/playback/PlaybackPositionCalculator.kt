package org.evoionosp.noveliq.domain.audiobook.playback

import javax.inject.Inject
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack

/**
 * Pure playback math over domain models. Contains no framework/player dependencies so the rules
 * (position translation, chapter navigation, completion) are unit-testable in isolation and can be
 * reused across the phone UI, Android Auto, and Wear.
 *
 * Positions are "absolute" seconds across the whole book unless stated otherwise; tracks are stored
 * in playback order, so a track's list index is also its media-item index.
 */
class PlaybackPositionCalculator @Inject constructor() {

    /** Translates an absolute position (seconds across the book) into a track index + in-track offset. */
    fun resolveSeekPosition(absoluteSeconds: Double, tracks: List<AudiobookTrack>): TrackPosition {
        if (tracks.isEmpty()) return TrackPosition(trackIndex = 0, offsetMs = 0L)
        val index = tracks.indexOfLast { absoluteSeconds >= it.startOffsetInSeconds }.coerceAtLeast(0)
        val offsetSeconds = absoluteSeconds - tracks[index].startOffsetInSeconds
        val offsetMs = (offsetSeconds * 1000).toLong().coerceAtLeast(0L)
        return TrackPosition(trackIndex = index, offsetMs = offsetMs)
    }

    /** The absolute position (seconds across the book) for an in-track position. */
    fun absolutePosition(trackIndex: Int, positionMs: Long, tracks: List<AudiobookTrack>): Double {
        if (trackIndex < 0) return 0.0
        val trackStart = tracks.getOrNull(trackIndex)?.startOffsetInSeconds ?: 0L
        return trackStart + positionMs.coerceAtLeast(0L) / 1000.0
    }

    /**
     * Total book duration in seconds, preferring the sum implied by the tracks and falling back to
     * the audiobook's own declared duration when track data is unavailable.
     */
    fun totalDurationSeconds(audiobook: Audiobook, tracks: List<AudiobookTrack>): Double {
        val fromTracks = tracks.lastOrNull()
            ?.let { it.startOffsetInSeconds + it.durationInSeconds }
            ?.toDouble()
            ?: 0.0
        return if (fromTracks > 0) fromTracks else audiobook.durationInSeconds?.toDouble() ?: 0.0
    }

    /**
     * Resolves the chapter a track belongs to by matching its start offset against chapter ranges,
     * falling back to the track's own title when there is no matching chapter metadata.
     */
    fun chapterTitleForTrack(track: AudiobookTrack, chapters: List<AudiobookChapter>): String {
        if (chapters.isEmpty()) return track.title
        val startSeconds = track.startOffsetInSeconds
        val chapter = chapters.firstOrNull { chapter ->
            startSeconds >= chapter.startInSeconds &&
                (chapter.endInSeconds == null || startSeconds < chapter.endInSeconds!!)
        }
        return chapter?.title ?: track.title
    }

    /** Start of the first chapter after [positionSeconds], or null if none / no chapter data. */
    fun nextChapterStart(positionSeconds: Double, chapters: List<AudiobookChapter>): Double? {
        if (chapters.isEmpty()) return null
        return chapters.firstOrNull { it.startInSeconds > positionSeconds }?.startInSeconds?.toDouble()
    }

    /**
     * Target for a "previous chapter" action, with the usual music-player behaviour: when more than
     * [PREVIOUS_CHAPTER_THRESHOLD_SECONDS] into the current chapter (or already at the first one),
     * restart the current chapter; otherwise jump to the start of the previous chapter.
     */
    fun previousChapterTarget(positionSeconds: Double, chapters: List<AudiobookChapter>): Double {
        if (chapters.isEmpty()) return 0.0
        val currentIndex = chapters.indexOfLast { it.startInSeconds <= positionSeconds }
        if (currentIndex < 0) return 0.0
        val currentStart = chapters[currentIndex].startInSeconds.toDouble()
        return if (positionSeconds - currentStart > PREVIOUS_CHAPTER_THRESHOLD_SECONDS || currentIndex == 0) {
            currentStart
        } else {
            chapters[currentIndex - 1].startInSeconds.toDouble()
        }
    }

    /** Whether an absolute position is close enough to the end to count the book as finished. */
    fun isFinished(absoluteSeconds: Double, totalSeconds: Double?): Boolean {
        val total = totalSeconds?.takeIf { it > 0 } ?: return false
        return absoluteSeconds >= total - FINISH_THRESHOLD_SECONDS
    }

    companion object {
        private const val FINISH_THRESHOLD_SECONDS = 5.0

        // Within this window into a chapter, "previous" restarts the current chapter; before it, it
        // goes to the previous chapter. (Media3's track-level default is 3s.)
        private const val PREVIOUS_CHAPTER_THRESHOLD_SECONDS = 20.0
    }
}
