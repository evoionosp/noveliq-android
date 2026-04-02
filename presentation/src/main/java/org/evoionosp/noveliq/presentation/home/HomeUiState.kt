package org.evoionosp.noveliq.presentation.home

import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.SyncStatus

data class HomeUiState(
    val libraries: List<AudiobookLibrary> = emptyList(),
    val selectedLibraryId: String? = null,
    val selectedLibraryName: String? = null,
    val audiobooks: List<Audiobook> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isRefreshing: Boolean = false
)
