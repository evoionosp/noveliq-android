package org.evoionosp.noveliq.presentation.home

import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.presentation.common.model.AudiobookUiModel
import org.evoionosp.noveliq.presentation.common.model.AuthorUiModel
import org.evoionosp.noveliq.presentation.common.model.LibraryUiModel

data class HomeUiState(
    val username: String = "",
    val libraries: List<LibraryUiModel> = emptyList(),
    val selectedLibraryId: String? = null,
    val selectedLibraryName: String? = null,
    val audiobooks: List<AudiobookUiModel> = emptyList(),
    val continueListening: List<AudiobookUiModel> = emptyList(),
    val recentlyAdded: List<AudiobookUiModel> = emptyList(),
    val discover: List<AudiobookUiModel> = emptyList(),
    val authors: List<AuthorUiModel> = emptyList(),
    val authorCount: Int = 0,
    val durationHours: Double = 0.0,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
