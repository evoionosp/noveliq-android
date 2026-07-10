package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.presentation.common.model.AudiobookUiModel
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.common.components.EmptyState
import org.evoionosp.noveliq.presentation.common.components.SectionBlock
import org.evoionosp.noveliq.presentation.navigation.LocalSnackbarHostState
import org.evoionosp.noveliq.presentation.navigation.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    bottomBarPadding: androidx.compose.ui.unit.Dp,
    onOpenAudiobook: (AudiobookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = bottomBarPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Custom header with greeting and settings button
                item {
                    HomeHeader(
                        username = state.username,
                        onOpenSettings = onOpenSettings
                    )
                }

                if (state.libraries.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.home_no_libraries),
                            subtitle = stringResource(R.string.home_no_libraries_hint),
                            modifier = Modifier.fillParentMaxHeight(0.8f)
                        )
                    }
                } else {
                    item {
                        SectionBlock(
                            title = stringResource(R.string.home_continue_listening)
                        ) {
                            if (state.continueListening.isEmpty()) {
                                PlaceholderSectionCard(
                                    title = stringResource(R.string.home_continue_empty),
                                    body = stringResource(R.string.home_continue_empty_hint)
                                )
                            } else {
                                HorizontalBookRow(
                                    audiobooks = state.continueListening,
                                    onOpenAudiobook = onOpenAudiobook
                                )
                            }
                        }
                    }

                    item {
                        SectionBlock(
                            title = stringResource(R.string.home_recently_added),
                        ) {
                            HorizontalBookRow(
                                audiobooks = state.recentlyAdded,
                                onOpenAudiobook = onOpenAudiobook
                            )
                        }
                    }

                    item {
                        SectionBlock(
                            title = stringResource(R.string.home_discover),
                        ) {
                            HorizontalBookRow(
                                audiobooks = state.discover,
                                onOpenAudiobook = onOpenAudiobook
                            )
                        }
                    }

                    item {
                        SectionBlock(
                            title = stringResource(R.string.home_stats),
                        ) {
                            StatsRow(
                                booksCount = state.audiobooks.size,
                                authorsCount = state.authorCount,
                                hoursCount = state.durationHours
                            )
                        }
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

@Composable
private fun HomeHeader(
    username: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = if (username.isNotBlank()) {
        "Hi, $username"
    } else {
        "Hi there"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(56.dp),  // 10-15% larger than standard 48.dp
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_icon_desc),
                modifier = Modifier.size(28.dp)  // Slightly larger icon
            )
        }
    }
}
