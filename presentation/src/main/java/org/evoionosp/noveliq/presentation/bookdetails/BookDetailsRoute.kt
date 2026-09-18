package org.evoionosp.noveliq.presentation.bookdetails

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.presentation.R

/**
 * Standalone book details page on the plain theme background — no sheet
 * behavior, no swipe gestures, no cover tint. Portrait shows a collapsing
 * cover header with its own back arrow; landscape keeps a plain app bar.
 * A normal nav destination; it exits via the arrow or system back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailsRoute(
    onBackClick: () -> Unit,
    viewModel: BookDetailsViewModel = hiltViewModel(),
    miniTrailPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audiobook = uiState.audiobook
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        modifier = modifier,
        // Portrait owns its top inset via the collapsing header, so only
        // landscape gets a plain app bar (back arrow, no title).
        topBar = {
            if (isLandscape) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.preferences_back),
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        // No layout reservation: the mini trail lives at the end of the
        // scrollable content instead, so unscrolled content still slides
        // under the overlay bar. Portrait also skips the top inset — the
        // collapsing header draws under the status bar with its own scrim.
        val contentModifier =
            if (isLandscape) {
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            }
        if (audiobook == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (isLandscape) {
            BookDetailsLandscape(
                audiobook = audiobook,
                bookProgress = uiState.bookProgress,
                chapters = uiState.chapters,
                inProgressSeconds = uiState.progressSeconds,
                isPlaying = uiState.isPlayingThisBook,
                onPlay = viewModel::playBook,
                onPlayChapter = viewModel::playChapter,
                miniTrailPadding = miniTrailPadding,
                modifier = contentModifier,
            )
        } else {
            BookDetailsContent(
                audiobook = audiobook,
                bookProgress = uiState.bookProgress,
                chapters = uiState.chapters,
                inProgressSeconds = uiState.progressSeconds,
                isPlaying = uiState.isPlayingThisBook,
                onPlayChapter = viewModel::playChapter,
                onTogglePlayPause = viewModel::togglePlayPause,
                onBackClick = onBackClick,
                miniTrailPadding = miniTrailPadding,
                modifier = contentModifier,
            )
        }
    }
}
