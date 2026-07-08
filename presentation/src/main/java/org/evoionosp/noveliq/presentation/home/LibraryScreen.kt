package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.navigation.LocalSnackbarHostState
import org.evoionosp.noveliq.presentation.navigation.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    accessToken: String,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: androidx.compose.ui.unit.Dp,
    onOpenAudiobook: (Audiobook) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeUiEvent.ShowMessage -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
            HomeUiEvent.SessionExpired -> {
                snackbarHostState.showSnackbar(context.getString(R.string.error_session_expired))
                onSessionExpired()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier
            .fillMaxSize()
            .background(homeBackgroundBrush())
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = bottomBarPadding + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RootScreenHeader(
                    title = stringResource(R.string.root_library),
                    onOpenSettings = onOpenSettings
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CatalogTopControls(
                    libraries = state.libraries,
                    selectedLibraryId = state.selectedLibraryId,
                    selectedLibraryName = state.selectedLibraryName,
                    syncStatus = state.syncStatus,
                    onLibrarySelected = viewModel::onLibrarySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            if (state.audiobooks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.home_no_books),
                        subtitle = stringResource(R.string.home_no_books_hint),
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp)
                    )
                }
            } else {
                items(state.audiobooks, key = { it.id }) { audiobook ->
                    AudiobookGridCard(
                        audiobook = audiobook,
                        accessToken = accessToken,
                        onClick = { onOpenAudiobook(audiobook) }
                    )
                }
            }
        }
    }
}
