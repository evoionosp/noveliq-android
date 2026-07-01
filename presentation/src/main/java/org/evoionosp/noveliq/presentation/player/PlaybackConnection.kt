package org.evoionosp.noveliq.presentation.player

import android.content.ComponentName
import android.content.Context
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
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
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

    fun playAudiobook(audiobook: Audiobook) {
        connectionScope.launch {
            val detail = audiobookRepository.observeAudiobookDetail(audiobook.libraryId, audiobook.id).first()
                ?: return@launch
            val session = sessionStore.session.first() ?: return@launch

            val mediaItems = detail.tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.index.toString())
                    .setUri(track.remoteUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(audiobook.author)
                            .setAlbumTitle(audiobook.title)
                            .build()
                    )
                    .build()
            }

            mediaController?.setMediaItems(mediaItems)
            mediaController?.prepare()
            mediaController?.play()
            _playbackState.update { it.copy(audiobook = audiobook) }
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
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
        _playbackState.update { it.copy(currentPositionMs = controller.currentPosition) }
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
            }
        }
    }
}
