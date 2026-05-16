package org.evoionosp.noveliq.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.presentation.R

@HiltViewModel
class AudiobookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val audiobookRepository: AudiobookRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(AudiobookDetailUiState())
    val uiState: StateFlow<AudiobookDetailUiState> = _uiState.asStateFlow()

    private val libraryId: String? = savedStateHandle["libraryId"]
    private val audiobookId: String? = savedStateHandle["audiobookId"]

    init {
        val currentLibraryId = libraryId
        val currentAudiobookId = audiobookId
        if (currentLibraryId != null && currentAudiobookId != null) {
            viewModelScope.launch {
                audiobookRepository.observeAudiobook(
                    libraryId = currentLibraryId,
                    audiobookId = currentAudiobookId
                ).collect { audiobook ->
                    _uiState.update { currentState ->
                        currentState.copy(audiobook = currentState.detail?.audiobook ?: audiobook)
                    }
                }
            }

            viewModelScope.launch {
                audiobookRepository.observeAudiobookDetail(
                    libraryId = currentLibraryId,
                    audiobookId = currentAudiobookId
                ).collect { detail ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            audiobook = detail?.audiobook ?: currentState.audiobook,
                            detail = detail,
                            chapters = detail?.chapters.orEmpty()
                        )
                    }
                }
            }

            viewModelScope.launch {
                refreshDetail(
                    libraryId = currentLibraryId,
                    audiobookId = currentAudiobookId
                )
            }
        }
    }

    private suspend fun refreshDetail(
        libraryId: String,
        audiobookId: String
    ) {
        val session = sessionStore.session.first() ?: return
        _uiState.update {
            it.copy(
                isLoadingChapters = true,
                chapterErrorMessageResId = null
            )
        }

        when (
            val result = audiobookRepository.refreshAudiobookDetail(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = libraryId,
                audiobookId = audiobookId
            )
        ) {
            is DomainResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoadingChapters = false,
                        chapterErrorMessageResId = null
                    )
                }
            }
            is DomainResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoadingChapters = false,
                        chapterErrorMessageResId = result.error.toMessageResId()
                    )
                }
            }
        }
    }

    private fun CatalogError.toMessageResId(): Int {
        return when (this) {
            CatalogError.AUTH -> R.string.audiobook_detail_chapters_auth
            CatalogError.NETWORK -> R.string.audiobook_detail_chapters_network
            CatalogError.CONNECTIVITY_UNAVAILABLE -> R.string.audiobook_detail_chapters_network
            CatalogError.NO_AUDIOBOOK_LIBRARIES -> R.string.audiobook_detail_chapters_empty
            CatalogError.NOT_FOUND -> R.string.audiobook_detail_chapters_missing
            CatalogError.UNKNOWN -> R.string.audiobook_detail_chapters_error
        }
    }
}
