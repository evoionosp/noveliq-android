package org.evoionosp.noveliq.presentation.player

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.playback.PlaybackState
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.utils.BookCoverArtwork
import org.evoionosp.noveliq.presentation.utils.SheetDragState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingScreen(
    onMinimize: () -> Unit,
    onOpenBookDetails: (Audiobook) -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
    sheetDrag: SheetDragState,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState = uiState.playback
    val audiobook = playbackState.audiobook ?: return
    // Saveable like the expanded state above: an open sheet must survive rotation too.
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showChaptersSheet by rememberSaveable { mutableStateOf(false) }
    var coverWidthPx by remember { mutableIntStateOf(0) }
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Overall book progress (0..1): live position over the playing book's total.
    val bookProgress =
        run {
            val total = uiState.totalSeconds
            val position = playbackState.currentBookPositionSeconds
            if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
        }

    val chapterTitle =
        uiState.chapters
            .lastOrNull {
                it.startInSeconds <= playbackState.currentBookPositionSeconds
            }?.title
    val speedLabel = "${"%.2f".format(playbackState.playbackSpeed)}x"

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                // Playback has no scrollable content, so the sheet owns the
                // drag detector directly.
                .pointerInput(Unit) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = {
                            if (sheetDrag.offsetPx.floatValue != 0f) {
                                sheetDrag.onDragEnd(tracker.calculateVelocity().y)
                            }
                        },
                        onDragCancel = { sheetDrag.onDragCancel() },
                        onVerticalDrag = { change, dragDelta ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            sheetDrag.dragBy(dragDelta)
                        },
                    )
                },
        // Transparent: the unified sheet owns the single tinted background
        // behind both this and the mini card, so they always match.
        color = Color.Transparent,
        // Explicit: Transparent has no scheme content color, so the default
        // would resolve implicit text/icon colors to black. The player text
        // role keeps them on-palette instead.
        contentColor = LocalPlayerColors.current.textPrimary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTopBar(onMinimize = onMinimize)

            if (isLandscape) {
                NowPlayingScreenLandscape(
                    modifier = Modifier.weight(1f),
                    audiobook = audiobook,
                    bookProgress = bookProgress,
                    playbackState = playbackState,
                    chapterTitle = chapterTitle,
                    speedLabel = speedLabel,
                    onSeekTo = viewModel::seekTo,
                    onSeekBackward = viewModel::seekBackward,
                    onSeekForward = viewModel::seekForward,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPreviousChapter = viewModel::previousChapter,
                    onNextChapter = viewModel::nextChapter,
                    onSpeedClick = { showSpeedSheet = true },
                    onChaptersClick = { showChaptersSheet = true },
                    onOpenBookDetails = onOpenBookDetails,
                )
            } else {
                // Book info block. Weighted so it fills the space above the controls; its content is
                // top-aligned, which pushes the flexible gap to sit between the info and the controls
                // (rather than leaving dead space at the bottom).
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Cover Image
                    BookCoverArtwork(
                        audiobook = audiobook,
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .aspectRatio(1f)
                                .onSizeChanged { coverWidthPx = it.width }
                                .clip(RoundedCornerShape(12.dp)),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Thin, read-only overall-book progress bar, matched to the cover width.
                    BookProgressBar(
                        progress = bookProgress,
                        modifier =
                            Modifier
                                .width(with(LocalDensity.current) { coverWidthPx.toDp() })
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title and Author
                    BookTitleBlock(
                        audiobook = audiobook,
                        textAlign = TextAlign.Center,
                        onClick = { onOpenBookDetails(audiobook) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PlayingTransport(
                    playbackState = playbackState,
                    chapterTitle = chapterTitle,
                    onSeekTo = viewModel::seekTo,
                    onSeekBackward = viewModel::seekBackward,
                    onSeekForward = viewModel::seekForward,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPreviousChapter = viewModel::previousChapter,
                    onNextChapter = viewModel::nextChapter,
                )

                Spacer(modifier = Modifier.height(24.dp))

                FooterActionsRow(
                    speedLabel = speedLabel,
                    onSpeedClick = { showSpeedSheet = true },
                    onChaptersClick = { showChaptersSheet = true },
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showSpeedSheet) {
        SpeedSheet(
            speed = playbackState.playbackSpeed,
            onSpeedChange = viewModel::setPlaybackSpeed,
            onDismiss = { showSpeedSheet = false },
        )
    }

    if (showChaptersSheet) {
        val currentChapterIndex =
            uiState.chapters.indexOfLast {
                it.startInSeconds <= playbackState.currentBookPositionSeconds
            }
        ChaptersSheet(
            chapters = uiState.chapters,
            currentChapterIndex = currentChapterIndex,
            isPlaying = playbackState.isPlaying,
            onPlayChapter = viewModel::playChapter,
            onDismiss = { showChaptersSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayingTransport(
    playbackState: PlaybackState,
    chapterTitle: String?,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    fun tick() = hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val pc = LocalPlayerColors.current

    if (!chapterTitle.isNullOrBlank()) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = pc.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    PlayerSeekBar(
        positionMs = playbackState.currentPositionMs,
        durationMs = playbackState.durationMs,
        isPlaying = playbackState.isPlaying,
        onSeekTo = onSeekTo,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                tick()
                onPreviousChapter()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.now_playing_previous_chapter),
                tint = pc.textPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        SeekButton(label = "15", onClick = onSeekBackward)
        MorphingPlayPauseButton(
            isPlaying = playbackState.isPlaying,
            onClick = {
                tick()
                onTogglePlayPause()
            },
            buttonSize = 96.dp,
            iconSize = 56.dp,
        )
        SeekButton(label = "30", onClick = onSeekForward)
        IconButton(
            onClick = {
                tick()
                onNextChapter()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.now_playing_next_chapter),
                tint = pc.textPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/**
 * Full-player top bar: a tonal down-arrow button like the Preferences back
 * button, plus the SemiBold "Now Playing" title. Shared by portrait and
 * landscape.
 */
@Composable
private fun PlayerTopBar(onMinimize: () -> Unit) {
    val pc = LocalPlayerColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onMinimize,
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = pc.containerSubtle,
                    contentColor = pc.accent,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.close),
            )
        }
        // Same rendering as a collapsed settings title.
        val titleStyle = MaterialTheme.typography.headlineMedium
        Text(
            text = stringResource(R.string.now_playing_title),
            modifier = Modifier.padding(start = 18.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                titleStyle.copy(
                    fontSize = titleStyle.fontSize * 0.8f,
                    lineHeight = titleStyle.fontSize * 0.8f * 1.1f,
                    fontWeight = FontWeight.Bold,
                ),
            color = pc.textPrimary,
        )
    }
}

@Composable
internal fun BookCoverArtwork(
    audiobook: Audiobook,
    modifier: Modifier = Modifier,
) {
    BookCoverArtwork(
        coverUrl = audiobook.coverUrl,
        title = audiobook.title,
        author = audiobook.author,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pc = LocalPlayerColors.current
    // Thin, read-only overall-book progress bar.
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = pc.accent,
        trackColor = pc.trackSubtle,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
internal fun BookTitleBlock(
    audiobook: Audiobook,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    // Null on the details page itself, where the title navigates nowhere.
    onClick: (() -> Unit)? = null,
) {
    val pc = LocalPlayerColors.current
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(
                onClickLabel = stringResource(R.string.now_playing_open_book_details),
                onClick = onClick,
            )
        } else {
            Modifier
        }
    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment =
            if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = audiobook.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = pc.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = audiobook.author,
            style = MaterialTheme.typography.titleMedium,
            color = pc.accent,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun FooterActionsRow(
    speedLabel: String,
    onSpeedClick: () -> Unit,
    onChaptersClick: () -> Unit,
) {
    val pc = LocalPlayerColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAction(title = stringResource(R.string.now_playing_sleep), icon = "Zz")
        PlayerAction(
            title = stringResource(R.string.now_playing_speed),
            icon = speedLabel,
            onClick = onSpeedClick,
        )
        Column(
            modifier = Modifier.clickable(onClick = onChaptersClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = null,
                tint = pc.textPrimary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.now_playing_chapters),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = pc.textSecondary,
            )
        }
    }
}
