package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    val authors = remember(state.audiobooks) {
        state.audiobooks
            .flatMap { audiobook ->
                audiobook.author.toAuthorNames().map { authorName -> authorName to audiobook }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (author, books) ->
                AuthorGridItem(
                    name = author,
                    bookCount = books.size,
                    photoUrl = books.firstOrNull()?.coverUrl
                )
            }
            .sortedBy { it.name.lowercase() }
    }

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
        topBar = {
            RootScreenHeader(
                title = stringResource(R.string.root_library),
                onOpenSettings = onOpenSettings
            )
        },
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
                        top = 8.dp,
                        bottom = bottomBarPadding + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item(span = { GridItemSpan(12) }) {
                        CatalogTopControls(
                            libraries = state.libraries,
                            selectedLibraryId = state.selectedLibraryId,
                            selectedLibraryName = state.selectedLibraryName,
                            syncStatus = state.syncStatus,
                            onLibrarySelected = viewModel::onLibrarySelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Books Section
                    item(span = { GridItemSpan(12) }) {
                        LibrarySectionHeader(
                            title = stringResource(R.string.stats_books),
                            subtitle = "${state.audiobooks.size} Books"
                        )
                    }

                    items(
                        items = state.audiobooks,
                        key = { it.id },
                        span = { GridItemSpan(4) } // 3 columns
                    ) { audiobook ->
                        AudiobookGridCard(
                            audiobook = audiobook,
                            accessToken = accessToken,
                            onClick = { onOpenAudiobook(audiobook) }
                        )
                    }

                    // Authors Section
                    item(span = { GridItemSpan(12) }) {
                        LibrarySectionHeader(
                            title = stringResource(R.string.stats_authors),
                            subtitle = "${authors.size} Authors"
                        )
                    }

                    if (authors.isEmpty()) {
                        item(span = { GridItemSpan(12) }) {
                            EmptyState(
                                title = stringResource(R.string.authors_empty_title),
                                subtitle = stringResource(R.string.authors_empty_hint),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
                            )
                        }
                    } else {
                        items(
                            items = authors,
                            key = { it.name },
                            span = { GridItemSpan(3) } // 4 columns
                        ) { author ->
                            AuthorCard(author = author, accessToken = accessToken)
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
