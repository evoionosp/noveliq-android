package org.evoionosp.noveliq.presentation.bookdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.noveliq.playback.PlaybackConnection
import org.evoionosp.noveliq.presentation.navigation.BOOK_DETAILS_ARG_AUDIOBOOK_ID
import org.evoionosp.noveliq.presentation.navigation.BOOK_DETAILS_ARG_LIBRARY_ID

/**
 * UI state for the standalone book details page. The book comes from the
 * cached detail for the navigated IDs; progress resolves to the live position
 * while this book is the one playing, and to the saved position otherwise.
 */
data class BookDetailsUiState(
    val audiobook: Audiobook? = null,
    val chapters: List<AudiobookChapter> = emptyList(),
    val progressSeconds: Double = 0.0,
    val totalSeconds: Double = 0.0,
    val isPlayingThisBook: Boolean = false,
) {
    val bookProgress: Float =
        if (totalSeconds > 0) {
            (progressSeconds / totalSeconds).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

/**
 * Progress to display on the details page: live position while this book is
 * the one playing, saved position otherwise. Pure so the rule stays testable.
 */
internal fun resolveDetailsProgress(
    isPlayingBook: Boolean,
    livePositionSeconds: Double,
    savedProgressSeconds: Double,
): Double = if (isPlayingBook) livePositionSeconds else savedProgressSeconds

@HiltViewModel
class BookDetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val playbackConnection: PlaybackConnection,
        private val observeAudiobookDetail: ObserveAudiobookDetailUseCase,
        private val refreshAudiobookDetail: RefreshAudiobookDetailUseCase,
        private val fetchPlaybackProgress: FetchPlaybackProgressUseCase,
        private val getValidSessionUseCase: GetValidSessionUseCase,
    ) : ViewModel() {
        private val libraryId: String = checkNotNull(savedStateHandle[BOOK_DETAILS_ARG_LIBRARY_ID])
        private val audiobookId: String = checkNotNull(savedStateHandle[BOOK_DETAILS_ARG_AUDIOBOOK_ID])

        private val book = MutableStateFlow<Audiobook?>(null)
        private val chapters = MutableStateFlow<List<AudiobookChapter>>(emptyList())
        private val savedProgress = MutableStateFlow(0.0)
        private val totalSeconds = MutableStateFlow(0.0)

        private var loadJob: Job? = null

        val uiState: StateFlow<BookDetailsUiState> =
            combine(
                playbackConnection.playbackState,
                book,
                chapters,
                savedProgress,
                totalSeconds,
            ) { playback, book, chapterList, saved, total ->
                val isPlayingBook = book != null && playback.audiobook?.id == book.id
                BookDetailsUiState(
                    audiobook = book,
                    chapters = chapterList,
                    progressSeconds =
                        resolveDetailsProgress(
                            isPlayingBook,
                            playback.currentBookPositionSeconds,
                            saved,
                        ),
                    totalSeconds = total,
                    isPlayingThisBook = isPlayingBook && playback.isPlaying,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailsUiState())

        init {
            loadBook()
        }

        /** Starts the book, or resumes it when it is already the active one. */
        fun playBook() {
            val book = book.value ?: return
            if (playbackConnection.playbackState.value.audiobook
                    ?.id == book.id
            ) {
                playbackConnection.play()
            } else {
                playbackConnection.playAudiobook(book)
            }
        }

        /** Pauses when this book is the one playing, otherwise starts/resumes it. */
        fun togglePlayPause() {
            val book = book.value ?: return
            val playback = playbackConnection.playbackState.value
            if (playback.audiobook?.id == book.id && playback.isPlaying) {
                playbackConnection.pause()
            } else {
                playBook()
            }
        }

        /**
         * Starts playback of this book from the given chapter. When this book
         * is already the active one, it just seeks; otherwise it starts it at
         * the chapter.
         */
        fun playChapter(chapter: AudiobookChapter) {
            val book = book.value ?: return
            val playing = playbackConnection.playbackState.value.audiobook
            if (playing?.id == book.id) {
                playbackConnection.seekToBookSeconds(chapter.startInSeconds.toDouble())
            } else {
                playbackConnection.playAudiobook(
                    book,
                    startPositionSeconds = chapter.startInSeconds.toDouble(),
                )
            }
        }

        private fun loadBook() {
            loadJob?.cancel()
            chapters.value = emptyList()

            loadJob =
                viewModelScope.launch {
                    val session = getValidSessionUseCase()

                    // Refresh detail so chapters (and tracks for playback) are current.
                    if (session != null) {
                        refreshAudiobookDetail(
                            baseUrl = session.baseUrl,
                            accessToken = session.accessToken,
                            libraryId = libraryId,
                            audiobookId = audiobookId,
                        )
                    }

                    // Keep the book, chapters + total duration in sync with the cached detail.
                    launch {
                        observeAudiobookDetail(libraryId, audiobookId)
                            .collect { detail ->
                                if (detail != null) {
                                    book.value = detail.audiobook
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

                    // Fetch saved progress for the progress bar and chapter marker.
                    if (session != null) {
                        val progress =
                            fetchPlaybackProgress(
                                baseUrl = session.baseUrl,
                                accessToken = session.accessToken,
                                audiobookId = audiobookId,
                            )
                        val resume = progress?.resumeSeconds
                        if (progress != null && resume != null) {
                            savedProgress.update { resume }
                            if (totalSeconds.value <= 0) {
                                totalSeconds.update { progress.durationSeconds ?: 0.0 }
                            }
                        }
                    }
                }
        }
    }
