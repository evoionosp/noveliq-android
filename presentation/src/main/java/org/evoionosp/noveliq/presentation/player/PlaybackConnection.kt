package org.evoionosp.noveliq.presentation.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.DomainResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audiobookRepository: AudiobookRepository,
    private val sessionStore: SessionStore
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Context for the currently-loaded book, used to sync progress to the server.
    private var currentAudiobookId: String? = null
    private var currentTracks: List<AudiobookTrack> = emptyList()
    private var currentTotalDurationSeconds: Double = 0.0
    private var secondsSinceServerSave: Int = 0

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get().apply {
                addListener(PlayerListener())
                updateState()
            }
        }, MoreExecutors.directExecutor())

        startProgressUpdateLoop()
    }

    fun playAudiobook(audiobook: Audiobook, startPositionSeconds: Double? = null) {
        connectionScope.launch {
            // Persist the currently-playing book's progress before switching away from it, so
            // returning to it later resumes from the right spot.
            saveCurrentProgressNow()

            val session = sessionStore.session.first() ?: return@launch

            // Ensure detail/tracks are cached before playing. The detail screen used to prefetch
            // these; now playback refreshes them itself on a cache miss.
            var resolvedDetail = audiobookRepository.observeAudiobookDetail(audiobook.libraryId, audiobook.id).first()
            if (resolvedDetail == null || resolvedDetail.tracks.isEmpty()) {
                audiobookRepository.refreshAudiobookDetail(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = audiobook.libraryId,
                    audiobookId = audiobook.id
                )
                resolvedDetail = audiobookRepository.observeAudiobookDetail(audiobook.libraryId, audiobook.id).first()
            }
            val detail = resolvedDetail?.takeIf { it.tracks.isNotEmpty() } ?: return@launch

            val artworkUri = audiobook.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)

            val mediaItems = detail.tracks.map { track ->
                val chapterTitle = chapterTitleForTrack(track, detail.chapters)
                MediaItem.Builder()
                    .setMediaId(track.index.toString())
                    .setUri(track.remoteUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            // Main line: the audiobook, not the underlying audio file name.
                            .setTitle(audiobook.title)
                            // Secondary line: the chapter for this track.
                            .setArtist(chapterTitle)
                            .setAlbumTitle(audiobook.title)
                            .setAlbumArtist(audiobook.author)
                            .setDisplayTitle(audiobook.title)
                            .setSubtitle(chapterTitle)
                            .apply { artworkUri?.let(::setArtworkUri) }
                            .build()
                    )
                    .build()
            }

            // Update the context used for progress syncing.
            currentAudiobookId = audiobook.id
            currentTracks = detail.tracks
            currentTotalDurationSeconds = computeTotalDurationSeconds(audiobook, detail.tracks)
            secondsSinceServerSave = 0

            // Determine the start position: an explicit one (e.g. a chosen chapter) wins;
            // otherwise resume from the saved server progress.
            val (startIndex, startOffsetMs) = when {
                startPositionSeconds != null ->
                    resolveSeekPosition(startPositionSeconds, detail.tracks)
                else -> {
                    val resume = fetchResumeProgress(session.baseUrl, session.accessToken, audiobook.id)
                    if (resume != null && !resume.isFinished && resume.currentTimeSeconds > 0) {
                        resolveSeekPosition(resume.currentTimeSeconds, detail.tracks)
                    } else {
                        0 to 0L
                    }
                }
            }

            val controller = mediaController ?: return@launch
            controller.setMediaItems(mediaItems, startIndex, startOffsetMs)
            controller.prepare()
            controller.play()
            _playbackState.update { it.copy(audiobook = audiobook) }

            // Persist immediately when starting at an explicit position so the resume point updates.
            if (startPositionSeconds != null) {
                saveCurrentProgressNow()
            }
        }
    }

    private suspend fun fetchResumeProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): PlaybackProgress? {
        return when (val result = audiobookRepository.fetchProgress(baseUrl, accessToken, audiobookId)) {
            is DomainResult.Success -> result.data
            is DomainResult.Failure -> null
        }
    }

    /**
     * Translates an absolute position (seconds across the whole book) into a media-item index and
     * an in-item offset in milliseconds. Media items are built in track order, so the track's list
     * position is also its media-item index.
     */
    private fun resolveSeekPosition(
        absoluteSeconds: Double,
        tracks: List<AudiobookTrack>
    ): Pair<Int, Long> {
        if (tracks.isEmpty()) return 0 to 0L
        val index = tracks.indexOfLast { absoluteSeconds >= it.startOffsetInSeconds }.coerceAtLeast(0)
        val offsetSeconds = absoluteSeconds - tracks[index].startOffsetInSeconds
        val offsetMs = (offsetSeconds * 1000).toLong().coerceAtLeast(0L)
        return index to offsetMs
    }

    private fun currentAbsoluteSeconds(controller: MediaController): Double {
        val index = controller.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return 0.0
        val trackStart = currentTracks.getOrNull(index)?.startOffsetInSeconds ?: 0L
        return trackStart + controller.currentPosition.coerceAtLeast(0L) / 1000.0
    }

    private fun computeTotalDurationSeconds(
        audiobook: Audiobook,
        tracks: List<AudiobookTrack>
    ): Double {
        val fromTracks = tracks.lastOrNull()
            ?.let { it.startOffsetInSeconds + it.durationInSeconds }
            ?.toDouble()
            ?: 0.0
        return if (fromTracks > 0) fromTracks else audiobook.durationInSeconds?.toDouble() ?: 0.0
    }

    /** Fire-and-forget progress save (used from callbacks that can't suspend). */
    private fun saveCurrentProgressAsync() {
        connectionScope.launch { saveCurrentProgressNow() }
    }

    private suspend fun saveCurrentProgressNow() {
        val controller = mediaController ?: return
        val audiobookId = currentAudiobookId ?: return
        val tracks = currentTracks
        if (tracks.isEmpty()) return

        val rawIndex = controller.currentMediaItemIndex
        if (rawIndex == C.INDEX_UNSET) return
        val index = rawIndex.coerceIn(tracks.indices)
        val track = tracks.getOrNull(index) ?: return

        val positionSeconds = controller.currentPosition.coerceAtLeast(0L) / 1000.0
        val absoluteSeconds = track.startOffsetInSeconds + positionSeconds
        val total = currentTotalDurationSeconds.takeIf { it > 0 }
        val isFinished = total != null && absoluteSeconds >= total - FINISH_THRESHOLD_SECONDS

        // Read the CURRENT session token; it rotates on refresh, so a cached one may be stale (401).
        val session = sessionStore.session.first() ?: return
        if (session.baseUrl.isBlank() || session.accessToken.isBlank()) return

        audiobookRepository.saveProgress(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            audiobookId = audiobookId,
            progress = PlaybackProgress(
                currentTimeSeconds = absoluteSeconds,
                durationSeconds = total,
                isFinished = isFinished
            )
        )
    }

    /**
     * Resolves the chapter a track belongs to by matching the track's start offset against the
     * chapter time ranges. Falls back to the track's own title when no chapter can be matched
     * (e.g. the book has no chapter metadata).
     */
    private fun chapterTitleForTrack(
        track: AudiobookTrack,
        chapters: List<AudiobookChapter>
    ): String {
        if (chapters.isEmpty()) return track.title
        val startSeconds = track.startOffsetInSeconds
        val chapter = chapters.firstOrNull { chapter ->
            startSeconds >= chapter.startInSeconds &&
                (chapter.endInSeconds == null || startSeconds < chapter.endInSeconds!!)
        }
        return chapter?.title ?: track.title
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
        saveCurrentProgressAsync()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /** Seeks to an absolute position (seconds across the whole book), e.g. a chapter start. */
    fun seekToBookSeconds(absoluteSeconds: Double) {
        val controller = mediaController ?: return
        if (currentTracks.isEmpty()) {
            controller.seekTo((absoluteSeconds * 1000).toLong().coerceAtLeast(0L))
            return
        }
        val (index, offsetMs) = resolveSeekPosition(absoluteSeconds, currentTracks)
        controller.seekTo(index, offsetMs)
        // Reflect the jump in the server progress.
        saveCurrentProgressAsync()
    }

    /** Jumps to the start of the next chapter after the current playback position. */
    fun skipToNextChapter(chapters: List<AudiobookChapter>) {
        val controller = mediaController ?: return
        if (chapters.isEmpty()) return
        val position = currentAbsoluteSeconds(controller)
        val next = chapters.firstOrNull { it.startInSeconds > position } ?: return
        seekToBookSeconds(next.startInSeconds.toDouble())
    }

    /**
     * Jumps to the previous chapter, with the usual music-player behaviour: if we're more than
     * [PREVIOUS_CHAPTER_THRESHOLD_SECONDS] into the current chapter, restart the current chapter;
     * otherwise go to the start of the previous chapter.
     */
    fun skipToPreviousChapter(chapters: List<AudiobookChapter>) {
        val controller = mediaController ?: return
        val position = currentAbsoluteSeconds(controller)
        if (chapters.isEmpty()) {
            seekToBookSeconds(0.0)
            return
        }
        val currentIndex = chapters.indexOfLast { it.startInSeconds <= position }
        if (currentIndex < 0) {
            seekToBookSeconds(0.0)
            return
        }
        val currentStart = chapters[currentIndex].startInSeconds.toDouble()
        val target = if (position - currentStart > PREVIOUS_CHAPTER_THRESHOLD_SECONDS || currentIndex == 0) {
            currentStart
        } else {
            chapters[currentIndex - 1].startInSeconds.toDouble()
        }
        seekToBookSeconds(target)
    }

    fun seekForward(amountMs: Long = 30000) {
        val controller = mediaController ?: return
        controller.seekTo(controller.currentPosition + amountMs)
    }

    fun seekBackward(amountMs: Long = 15000) {
        val controller = mediaController ?: return
        controller.seekTo((controller.currentPosition - amountMs).coerceAtLeast(0))
    }

    private fun startProgressUpdateLoop() {
        connectionScope.launch {
            while (true) {
                updateProgress()
                delay(1000)
            }
        }
    }

    private fun updateProgress() {
        val controller = mediaController ?: return
        val absoluteSeconds = currentAbsoluteSeconds(controller)
        _playbackState.update {
            it.copy(
                currentPositionMs = controller.currentPosition,
                currentBookPositionSeconds = absoluteSeconds
            )
        }

        // Periodically push progress to the server while actively playing.
        if (controller.isPlaying && currentAudiobookId != null) {
            secondsSinceServerSave++
            if (secondsSinceServerSave >= SERVER_SAVE_INTERVAL_SECONDS) {
                secondsSinceServerSave = 0
                saveCurrentProgressAsync()
            }
        }
    }

    // Inside PlaybackConnection class
    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.setPlaybackSpeed(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    // Update updateState() to capture initial speed
    private fun updateState() {
        val controller = mediaController ?: return
        _playbackState.update {
            it.copy(
                isPlaying = controller.isPlaying,
                durationMs = controller.duration,
                currentPositionMs = controller.currentPosition,
                playbackSpeed = controller.playbackParameters.speed // Added this
            )
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                updateState()
                if (player.playbackState == Player.STATE_ENDED) {
                    saveCurrentProgressAsync()
                }
            }
        }
    }

    companion object {
        private const val SERVER_SAVE_INTERVAL_SECONDS = 15
        private const val FINISH_THRESHOLD_SECONDS = 5.0
        // Within this window into a chapter, the "previous" action restarts the current chapter;
        // before it, it goes to the previous chapter. (Media3's track-level default is 3s.)
        private const val PREVIOUS_CHAPTER_THRESHOLD_SECONDS = 20.0
    }
}
