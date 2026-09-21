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
import org.evoionosp.noveliq.domain.settings.AppSettings
import org.evoionosp.noveliq.domain.settings.AppSettingsStore
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

/**
 * Book position where sleep-at-end-of-chapter fires: the current chapter's
 * own end when known and ahead, else the next chapter's start, else the end
 * of the book. Null when nothing bounds the current chapter (no chapters and
 * no total, or already at the end), in which case the UI offers no chapter
 * mode. Pure so the boundary rule stays testable.
 */
internal fun sleepChapterTargetSeconds(
    chapters: List<AudiobookChapter>,
    positionSeconds: Double,
    totalSeconds: Double,
): Double? {
    val index = chapters.indexOfLast { it.startInSeconds <= positionSeconds }
    chapters
        .getOrNull(index)
        ?.endInSeconds
        ?.toDouble()
        ?.takeIf { it > positionSeconds }
        ?.let { return it }
    chapters
        .getOrNull(index + 1)
        ?.startInSeconds
        ?.toDouble()
        ?.takeIf { it > positionSeconds }
        ?.let { return it }
    return totalSeconds.takeIf { it > positionSeconds }
}

@HiltViewModel
class NowPlayingViewModel
    @Inject
    constructor(
        private val playbackConnection: PlaybackConnection,
        private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
        private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
        private val appSettingsStore: AppSettingsStore,
    ) : ViewModel() {
        val playbackState: StateFlow<PlaybackState> = playbackConnection.playbackState

        /** Last sleep-timer duration picked, surviving app kill; the armed timer itself does not. */
        val sleepTimerMinutes: StateFlow<Int> =
            appSettingsStore.settings
                .map { it.sleepTimerMinutes }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    AppSettings.DEFAULT_SLEEP_TIMER_MINUTES,
                )

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

        fun armSleepTimer(minutes: Int) {
            playbackConnection.startSleepTimer(minutes)
        }

        /** Persists the staged duration without arming, so the sheet reopens where it left off. */
        fun saveSleepTimerMinutes(minutes: Int) {
            viewModelScope.launch {
                appSettingsStore.setSleepTimerMinutes(minutes)
            }
        }

        /** Arms sleep-at-chapter-end; no-op when no chapter boundary is known. */
        fun armSleepEndOfChapter() {
            val target =
                sleepChapterTargetSeconds(
                    chapters.value,
                    playbackState.value.currentBookPositionSeconds,
                    totalSeconds.value,
                ) ?: return
            playbackConnection.startSleepAtChapterEnd(target)
        }

        fun cancelSleepTimer() {
            playbackConnection.cancelSleepTimer()
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
