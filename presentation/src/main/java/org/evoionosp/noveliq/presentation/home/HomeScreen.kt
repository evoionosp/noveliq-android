package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
fun HomeScreen(
    accessToken: String,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: Dp,
    onOpenAudiobook: (Audiobook) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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

    val continueListening = state.continueListening
    val recentlyAdded = state.audiobooks.take(12)
    val discover = state.audiobooks
        .sortedBy { it.title.lowercase() }
        .filterIndexed { index, _ -> index % 2 == 0 }
        .take(12)
    val authorCount = state.audiobooks
        .flatMap { it.author.toAuthorNames() }
        .distinct()
        .size
    val durationHours = state.audiobooks.sumOf { it.durationInSeconds ?: 0L } / 3600.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RootScreenHeader(
                title = stringResource(R.string.root_home),
                onOpenSettings = onOpenSettings
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.libraries.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_no_libraries),
                subtitle = stringResource(R.string.home_no_libraries_hint),
                modifier = Modifier
                    .fillMaxSize()
                    .background(homeBackgroundBrush())
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .background(homeBackgroundBrush())
                .padding(innerPadding),
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomBarPadding + 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CatalogTopControls(
                        libraries = state.libraries,
                        selectedLibraryId = state.selectedLibraryId,
                        selectedLibraryName = state.selectedLibraryName,
                        syncStatus = state.syncStatus,
                        onLibrarySelected = viewModel::onLibrarySelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                item {
                    SectionBlock(
                        title = stringResource(R.string.home_continue_listening),
                        subtitle = stringResource(R.string.home_continue_listening_hint)
                    ) {
                        if (continueListening.isEmpty()) {
                            PlaceholderSectionCard(
                                title = stringResource(R.string.home_continue_empty),
                                body = stringResource(R.string.home_continue_empty_hint)
                            )
                        } else {
                            HorizontalBookRow(
                                audiobooks = continueListening,
                                accessToken = accessToken,
                                onOpenAudiobook = onOpenAudiobook
                            )
                        }
                    }
                }

                item {
                    SectionBlock(
                        title = stringResource(R.string.home_recently_added),
                        subtitle = stringResource(R.string.home_recently_added_hint)
                    ) {
                        HorizontalBookRow(
                            audiobooks = recentlyAdded,
                            accessToken = accessToken,
                            onOpenAudiobook = onOpenAudiobook
                        )
                    }
                }

                item {
                    SectionBlock(
                        title = stringResource(R.string.home_discover),
                        subtitle = stringResource(R.string.home_discover_hint)
                    ) {
                        HorizontalBookRow(
                            audiobooks = discover,
                            accessToken = accessToken,
                            onOpenAudiobook = onOpenAudiobook
                        )
                    }
                }

                item {
                    SectionBlock(
                        title = stringResource(R.string.home_stats),
                        subtitle = stringResource(R.string.home_stats_hint)
                    ) {
                        StatsRow(
                            booksCount = state.audiobooks.size,
                            authorsCount = authorCount,
                            hoursCount = durationHours
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun homeBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest
        )
    )
}
