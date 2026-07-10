package org.evoionosp.noveliq.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.session.usecase.GetCurrentSessionUseCase
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.noveliq.playback.PlaybackConnection
import org.evoionosp.noveliq.playback.PlaybackState
import javax.inject.Inject

/**
 * UI state for the Now Playing screen. The screen renders one of two states, derived from whether
 * the book being *viewed* is also the book currently *playing*:
 *  - at-a-glance: viewing a book that is not the active one (preview; playback untouched)
 *  - playing: viewing the active book (full transport controls)
 */
data class NowPlayingUiState(
    val playback: PlaybackState = PlaybackState(),
    val viewedAudiobook: Audiobook? = null,
    val viewedProgressSeconds: Double = 0.0,
    val viewedTotalSeconds: Double = 0.0,
    val viewedHasProgress: Boolean = false,
    val chapters: List<AudiobookChapter> = emptyList()
) {
    val isGlance: Boolean
        get() = viewedAudiobook != null && viewedAudiobook.id != playback.audiobook?.id
}

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackConnection: PlaybackConnection,
    private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
    private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
    private val fetchPlaybackProgress: FetchPlaybackProgressUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState

    private val viewedAudiobook = MutableStateFlow<Audiobook?>(null)
    private val glance = MutableStateFlow(GlanceData())
    private val chapters = MutableStateFlow<List<AudiobookChapter>>(emptyList())

    private var viewedJob: Job? = null

    val uiState: StateFlow<NowPlayingUiState> = combine(
        playbackConnection.playbackState,
        viewedAudiobook,
        glance,
        chapters
    ) { playback, viewed, glanceData, chapterList ->
        NowPlayingUiState(
            playback = playback,
            viewedAudiobook = viewed,
            viewedProgressSeconds = glanceData.progressSeconds,
            viewedTotalSeconds = glanceData.totalSeconds,
            viewedHasProgress = glanceData.hasProgress,
            chapters = chapterList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

    /** Preview a book (from the catalog). Does not change playback. */
    fun openAudiobook(audiobook: Audiobook) {
        viewedAudiobook.value = audiobook
        loadViewed(audiobook)
    }

    /** View the currently-playing book (e.g. expanding from the mini bar). */
    fun viewCurrentlyPlaying() {
        val playing = playbackConnection.playbackState.value.audiobook ?: return
        viewedAudiobook.value = playing
        loadViewed(playing)
    }

    /** Start playing the previewed book, switching the screen into the playing state. */
    fun playViewedAudiobook() {
        val audiobook = viewedAudiobook.value ?: return
        playbackConnection.playAudiobook(audiobook)
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            playbackConnection.pause()
        } else {
            playbackConnection.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackConnection.seekTo(positionMs)
    }

    fun seekForward() {
        playbackConnection.seekForward()
    }

    fun seekBackward() {
        playbackConnection.seekBackward()
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackConnection.setPlaybackSpeed(speed)
    }

    fun nextChapter() {
        playbackConnection.skipToNextChapter(chapters.value)
    }

    fun previousChapter() {
        playbackConnection.skipToPreviousChapter(chapters.value)
    }

    /**
     * Starts playback of the viewed book from the given chapter and updates progress. If the viewed
     * book is already the active one, it just seeks; otherwise it starts it at the chapter.
     */
    fun playChapter(chapter: AudiobookChapter) {
        val viewed = viewedAudiobook.value ?: return
        val playing = playbackConnection.playbackState.value.audiobook
        if (playing?.id == viewed.id) {
            playbackConnection.seekToBookSeconds(chapter.startInSeconds.toDouble())
        } else {
            playbackConnection.playAudiobook(viewed, startPositionSeconds = chapter.startInSeconds.toDouble())
        }
    }

    private fun loadViewed(audiobook: Audiobook) {
        viewedJob?.cancel()
        chapters.value = emptyList()
        glance.value = GlanceData(totalSeconds = audiobook.durationInSeconds?.toDouble() ?: 0.0)

        viewedJob = viewModelScope.launch {
            val session = getCurrentSessionUseCase()

            // Refresh detail so chapters (and tracks for playback) are current.
            if (session != null) {
                refreshAudiobookDetail(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = audiobook.libraryId,
                    audiobookId = audiobook.id
                )
            }

            // Keep chapters + total duration in sync with the cached detail.
            launch {
                observeAudiobookDetail(audiobook.libraryId, audiobook.id)
                    .collect { detail ->
                        if (detail != null) {
                            chapters.value = detail.chapters
                            val total = detail.audiobook.durationInSeconds?.toDouble()
                                ?: detail.tracks.lastOrNull()
                                    ?.let { (it.startOffsetInSeconds + it.durationInSeconds).toDouble() }
                            if (total != null && total > 0) {
                                glance.update { it.copy(totalSeconds = total) }
                            }
                        }
                    }
            }

            // Fetch saved progress for the remaining-time line.
            if (session != null) {
                val progress = fetchPlaybackProgress(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    audiobookId = audiobook.id
                )
                val resume = progress?.resumeSeconds
                if (progress != null && resume != null) {
                    glance.update {
                        it.copy(
                            progressSeconds = resume,
                            hasProgress = true,
                            totalSeconds = if (it.totalSeconds > 0) {
                                it.totalSeconds
                            } else {
                                progress.durationSeconds ?: 0.0
                            }
                        )
                    }
                }
            }
        }
    }

    private data class GlanceData(
        val progressSeconds: Double = 0.0,
        val totalSeconds: Double = 0.0,
        val hasProgress: Boolean = false
    )
}
