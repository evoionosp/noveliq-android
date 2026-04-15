package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
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
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
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
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )
                    )
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
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
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
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
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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

private data class AuthorGridItem(
    val name: String,
    val bookCount: Int,
    val photoUrl: String?
)

@Composable
fun AuthorsScreen(
    accessToken: String,
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
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
                title = stringResource(R.string.root_authors),
                onOpenSettings = onOpenSettings
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            if (authors.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.authors_empty_title),
                    subtitle = stringResource(R.string.authors_empty_hint),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
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
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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

                    items(authors, key = { it.name }) { author ->
                        AuthorCard(author = author, accessToken = accessToken)
                    }
                }
            }
        }
    }
}

private fun String.toAuthorNames(): List<String> {
    return split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Unknown Author") }
}

@Composable
private fun RootScreenHeader(
    title: String,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_icon_desc)
                )
            }
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorizontalBookRow(
    audiobooks: List<Audiobook>,
    accessToken: String,
    onOpenAudiobook: (Audiobook) -> Unit
) {
    if (audiobooks.isEmpty()) {
        PlaceholderSectionCard(
            title = stringResource(R.string.home_section_empty_title),
            body = stringResource(R.string.home_section_empty_body)
        )
        return
    }

    HorizontalUncontainedCarousel(
        state = rememberCarouselState { audiobooks.size },
        modifier = Modifier.fillMaxWidth(),
        itemWidth = 150.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { index ->
        AudiobookCarouselCard(
            audiobook = audiobooks[index],
            accessToken = accessToken,
            onClick = { onOpenAudiobook(audiobooks[index]) }
        )
    }
}

@Composable
private fun PlaceholderSectionCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(
    booksCount: Int,
    authorsCount: Int,
    hoursCount: Double
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = stringResource(R.string.stats_books),
                value = booksCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_authors),
                value = authorsCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_hours),
                value = String.format("%.1f", hoursCount),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudiobookCarouselCard(
    audiobook: Audiobook,
    accessToken: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(304.dp)
    ) {
        AudiobookGridCard(
            audiobook = audiobook,
            accessToken = accessToken,
            onClick = onClick,
            coverAspectRatio = 0.72f
        )
    }
}

@Composable
private fun AuthorCard(
    author: AuthorGridItem,
    accessToken: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            if (author.photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(author.photoUrl)
                            .addHeader("Authorization", "Bearer $accessToken")
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    ),
                    contentDescription = author.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = author.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.authors_book_count, author.bookCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
