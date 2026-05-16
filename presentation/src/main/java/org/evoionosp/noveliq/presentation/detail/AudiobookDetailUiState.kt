package org.evoionosp.noveliq.presentation.detail

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail

data class AudiobookDetailUiState(
    val audiobook: Audiobook? = null,
    val detail: AudiobookDetail? = null,
    val chapters: List<AudiobookChapter> = emptyList(),
    val isLoadingChapters: Boolean = false,
    val chapterErrorMessageResId: Int? = null
)
