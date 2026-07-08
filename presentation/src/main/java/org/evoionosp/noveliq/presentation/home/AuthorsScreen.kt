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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.navigation.LocalSnackbarHostState
import org.evoionosp.noveliq.presentation.navigation.ObserveAsEvents

@Composable
fun AuthorsScreen(
    accessToken: String,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: androidx.compose.ui.unit.Dp,
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .background(homeBackgroundBrush()),
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
                title = stringResource(R.string.root_authors),
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

        if (authors.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = stringResource(R.string.authors_empty_title),
                    subtitle = stringResource(R.string.authors_empty_hint),
                    modifier = Modifier.fillMaxWidth().padding(top = 100.dp)
                )
            }
        } else {
            items(authors, key = { it.name }) { author ->
                AuthorCard(author = author, accessToken = accessToken)
            }
        }
    }
}
