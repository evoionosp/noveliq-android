package org.evoionosp.noveliq.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.evoionosp.noveliq.presentation.common.model.AudiobookUiModel
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.common.components.EmptyState
import org.evoionosp.noveliq.presentation.common.model.LibraryUiModel
import org.evoionosp.noveliq.presentation.home.ForegroundRefreshEffect
import org.evoionosp.noveliq.presentation.home.HomeUiEvent
import org.evoionosp.noveliq.presentation.home.HomeViewModel
import org.evoionosp.noveliq.presentation.home.AudiobookGridCard
import org.evoionosp.noveliq.presentation.home.AudiobookListCard
import org.evoionosp.noveliq.presentation.home.homeBackgroundBrush
import org.evoionosp.noveliq.presentation.navigation.LocalSnackbarHostState
import org.evoionosp.noveliq.presentation.navigation.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: androidx.compose.ui.unit.Dp,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    var isGridView by rememberSaveable { mutableStateOf(true) }

    ForegroundRefreshEffect(onForeground = viewModel::refreshSilently)

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .background(homeBackgroundBrush())
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (state.audiobooks.isEmpty() && state.libraries.isNotEmpty()) {
                EmptyState(
                    title = stringResource(R.string.home_no_books),
                    subtitle = stringResource(R.string.home_no_books_hint),
                    modifier = Modifier.fillMaxSize().padding(top = 100.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(12),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = bottomBarPadding + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isGridView) 24.dp else 12.dp)
                ) {
                    // Custom header with library dropdown and settings button
                    item(span = { GridItemSpan(12) }) {
                        LibraryHeader(
                            libraries = state.libraries,
                            selectedLibraryId = state.selectedLibraryId,
                            selectedLibraryName = state.selectedLibraryName,
                            syncStatus = state.syncStatus,
                            onLibrarySelected = viewModel::onLibrarySelected,
                            onOpenSettings = onOpenSettings,
                        )
                    }

                    // Books Section
                    item(span = { GridItemSpan(12) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LibrarySectionHeader(
                                title = stringResource(R.string.stats_books),
                                subtitle = "${state.audiobooks.size} Books",
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalIconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.FormatListBulleted else Icons.Rounded.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = state.audiobooks,
                        key = { it.id },
                        span = { GridItemSpan(if (isGridView) 4 else 12) }
                    ) { audiobook ->
                        if (isGridView) {
                            AudiobookGridCard(
                                audiobook = audiobook,
                                onClick = { onOpenAudiobook(audiobook) }
                            )
                        } else {
                            AudiobookListCard(
                                audiobook = audiobook,
                                onClick = { onOpenAudiobook(audiobook) }
                            )
                        }
                    }

                    // Authors Section
                    item(span = { GridItemSpan(12) }) {
                        LibrarySectionHeader(
                            title = stringResource(R.string.stats_authors),
                            subtitle = "${state.authors.size} Authors"
                        )
                    }

                    if (state.authors.isEmpty()) {
                        item(span = { GridItemSpan(12) }) {
                            EmptyState(
                                title = stringResource(R.string.authors_empty_title),
                                subtitle = stringResource(R.string.authors_empty_hint),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                            )
                        }
                    } else {
                        items(
                            items = state.authors,
                            key = { it.name },
                            span = { GridItemSpan(3) }
                        ) { author ->
                            AuthorCard(author = author)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryHeader(
    libraries: List<LibraryUiModel>,
    selectedLibraryId: String?,
    selectedLibraryName: String?,
    syncStatus: SyncStatus,
    onLibrarySelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Library dropdown with 2-line format - wrap content with min width
            LibraryDropdown(
                modifier = Modifier.fillMaxWidth().weight(1f),
                libraries = libraries,
                selectedLibraryId = selectedLibraryId,
                selectedLibraryName = selectedLibraryName,
                syncStatus = syncStatus,
                onLibrarySelected = onLibrarySelected
            )


        // Settings button only
        FilledTonalIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_icon_desc),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
