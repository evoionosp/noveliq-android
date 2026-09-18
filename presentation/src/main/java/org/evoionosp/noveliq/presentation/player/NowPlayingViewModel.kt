package org.evoionosp.noveliq.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.noveliq.playback.PlaybackConnection
import org.evoionosp.noveliq.playback.PlaybackState

/**
 * UI state for the Now Playing screen. Playing-only: the screen always shows
 * the active book from [PlaybackState]; book browsing lives on the standalone
 * details page. Chapter detail for the playing book loads automatically, so
 * transport (chapters sheet, chapter skip, progress bar) always has data.
 */
data class NowPlayingUiState(
    val playback: PlaybackState = PlaybackState(),
    val totalSeconds: Double = 0.0,
    val chapters: List<AudiobookChapter> = emptyList(),
)

@HiltViewModel
class NowPlayingViewModel
    @Inject
    constructor(
        private val playbackConnection: PlaybackConnection,
        private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
        private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
    ) : ViewModel() {
        val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState

        private val totalSeconds = MutableStateFlow(0.0)
        private val chapters = MutableStateFlow<List<AudiobookChapter>>(emptyList())

        private var detailJob: Job? = null

        val uiState: StateFlow<NowPlayingUiState> =
            combine(
                playbackConnection.playbackState,
                totalSeconds,
                chapters,
            ) { playback, total, chapterList ->
                NowPlayingUiState(
                    playback = playback,
                    totalSeconds = total,
                    chapters = chapterList,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

        init {
            viewModelScope.launch {
                playbackConnection.playbackState
                    .map { it.audiobook }
                    .distinctUntilChangedBy { it?.id }
                    .collect { audiobook ->
                        if (audiobook != null) {
                            loadPlaying(audiobook)
                        } else {
                            detailJob?.cancel()
                            chapters.value = emptyList()
                            totalSeconds.value = 0.0
                        }
                    }
            }
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

        /** Seeks the playing book to the given chapter. */
        fun playChapter(chapter: AudiobookChapter) {
            playbackConnection.seekToBookSeconds(chapter.startInSeconds.toDouble())
        }

        private fun loadPlaying(audiobook: Audiobook) {
            detailJob?.cancel()
            chapters.value = emptyList()
            totalSeconds.value = audiobook.durationInSeconds?.toDouble() ?: 0.0

            detailJob =
                viewModelScope.launch {
                    val session = getValidSessionUseCase()

                    // Refresh detail so chapters (and tracks for playback) are current.
                    if (session != null) {
                        refreshAudiobookDetail(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = audiobook.libraryId,
                            audiobookId = audiobook.id,
                        )
                    }

                    // Keep chapters + total duration in sync with the cached detail.
                    launch {
                        observeAudiobookDetail(audiobook.libraryId, audiobook.id)
                            .collect { detail ->
                                if (detail != null) {
                                    chapters.value = detail.chapters
                                    val total =
                                        detail.audiobook.durationInSeconds?.toDouble()
                                            ?: detail.tracks
                                                .lastOrNull()
                                                ?.let {
                                                    (it.startOffsetInSeconds + it.durationInSeconds)
                                                        .toDouble()
                                                }
                                    if (total != null && total > 0) {
                                        totalSeconds.update { total }
                                    }
                                }
                            }
                    }
                }
        }
    }
