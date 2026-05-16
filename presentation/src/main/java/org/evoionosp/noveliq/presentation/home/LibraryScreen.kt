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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    accessToken: String,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: Dp,
    onOpenAudiobook: (Audiobook) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowMessage -> snackbarHostState.showSnackbar(
                    context.getString(event.messageResId)
                )
                HomeUiEvent.SessionExpired -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_session_expired))
                    onSessionExpired()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RootScreenHeader(
                title = stringResource(R.string.root_library),
                onOpenSettings = onOpenSettings
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .background(homeBackgroundBrush())
                .padding(innerPadding),
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh
        ) {
            if (state.audiobooks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.home_no_books),
                    subtitle = stringResource(R.string.home_no_books_hint),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomBarPadding + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
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
}
