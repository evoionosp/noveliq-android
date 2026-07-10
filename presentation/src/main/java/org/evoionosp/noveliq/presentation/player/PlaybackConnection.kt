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
import org.evoionosp.noveliq.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.noveliq.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.PreparePlaybackUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.SavePlaybackProgressUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3 adapter: owns the [MediaController] connection and translates UI intents into player
 * commands. It holds no playback rules of its own — position math, chapter navigation, resume, and
 * progress-save policy all live in the domain layer ([PlaybackPositionCalculator] and the injected
 * use-cases). This keeps the adapter thin and the rules reusable across phone/Auto/Wear.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val calculator: PlaybackPositionCalculator,
    private val preparePlayback: PreparePlaybackUseCase,
    private val fetchPlaybackProgress: FetchPlaybackProgressUseCase,
    private val savePlaybackProgress: SavePlaybackProgressUseCase
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

            val detail = preparePlayback(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = audiobook.libraryId,
                audiobookId = audiobook.id
            ) ?: return@launch

            val artworkUri = audiobook.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)

            val mediaItems = detail.tracks.map { track ->
                val chapterTitle = calculator.chapterTitleForTrack(track, detail.chapters)
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
            currentTotalDurationSeconds = calculator.totalDurationSeconds(audiobook, detail.tracks)
            secondsSinceServerSave = 0

            // Determine the start position: an explicit one (e.g. a chosen chapter) wins; otherwise
            // resume from the saved server progress.
            val startSeconds = startPositionSeconds
                ?: fetchPlaybackProgress(session.baseUrl, session.accessToken, audiobook.id)?.resumeSeconds
                ?: 0.0
            val start = calculator.resolveSeekPosition(startSeconds, detail.tracks)

            val controller = mediaController ?: return@launch
            controller.setMediaItems(mediaItems, start.trackIndex, start.offsetMs)
            controller.prepare()
            controller.play()
            _playbackState.update { it.copy(audiobook = audiobook) }

            // Persist immediately when starting at an explicit position so the resume point updates.
            if (startPositionSeconds != null) {
                saveCurrentProgressNow()
            }
        }
    }

    /** The current absolute position (seconds across the book), derived via the domain calculator. */
    private fun currentAbsoluteSeconds(controller: MediaController): Double {
        val index = controller.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return 0.0
        return calculator.absolutePosition(index, controller.currentPosition, currentTracks)
    }

    /** Fire-and-forget progress save (used from callbacks that can't suspend). */
    private fun saveCurrentProgressAsync() {
        connectionScope.launch { saveCurrentProgressNow() }
    }

    private suspend fun saveCurrentProgressNow() {
        val controller = mediaController ?: return
        val audiobookId = currentAudiobookId ?: return
        if (currentTracks.isEmpty()) return
        if (controller.currentMediaItemIndex == C.INDEX_UNSET) return

        val absoluteSeconds = currentAbsoluteSeconds(controller)

        // Read the CURRENT session token; it rotates on refresh, so a cached one may be stale (401).
        val session = sessionStore.session.first() ?: return
        if (session.baseUrl.isBlank() || session.accessToken.isBlank()) return

        savePlaybackProgress(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            audiobookId = audiobookId,
            absoluteSeconds = absoluteSeconds,
            totalSeconds = currentTotalDurationSeconds.takeIf { it > 0 }
        )
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
        val target = calculator.resolveSeekPosition(absoluteSeconds, currentTracks)
        controller.seekTo(target.trackIndex, target.offsetMs)
        // Reflect the jump in the server progress.
        saveCurrentProgressAsync()
    }

    /** Jumps to the start of the next chapter after the current playback position. */
    fun skipToNextChapter(chapters: List<AudiobookChapter>) {
        val controller = mediaController ?: return
        val position = currentAbsoluteSeconds(controller)
        val next = calculator.nextChapterStart(position, chapters) ?: return
        seekToBookSeconds(next)
    }

    /** Jumps to the previous chapter (restarting the current one if we're well into it). */
    fun skipToPreviousChapter(chapters: List<AudiobookChapter>) {
        val controller = mediaController ?: return
        val position = currentAbsoluteSeconds(controller)
        seekToBookSeconds(calculator.previousChapterTarget(position, chapters))
    }

    fun seekForward(amountMs: Long = SEEK_FORWARD_MS) {
        val controller = mediaController ?: return
        controller.seekTo(controller.currentPosition + amountMs)
    }

    fun seekBackward(amountMs: Long = SEEK_BACKWARD_MS) {
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

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.setPlaybackSpeed(speed)
        _playbackState.update { it.copy(playbackSpeed = speed) }
    }

    private fun updateState() {
        val controller = mediaController ?: return
        _playbackState.update {
            it.copy(
                isPlaying = controller.isPlaying,
                durationMs = controller.duration,
                currentPositionMs = controller.currentPosition,
                playbackSpeed = controller.playbackParameters.speed
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

    private companion object {
        private const val SERVER_SAVE_INTERVAL_SECONDS = 15
        private const val SEEK_FORWARD_MS = 30_000L
        private const val SEEK_BACKWARD_MS = 15_000L
    }
}
