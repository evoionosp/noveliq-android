package org.evoionosp.noveliq.presentation.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
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
    viewModel: NowPlayingViewModel = hiltViewModel(),
    sheetDrag: SheetDragState,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audiobook = uiState.viewedAudiobook ?: return
    val playbackState = uiState.playback
    // Saveable like the expanded state above: an open sheet must survive rotation too.
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showChaptersSheet by rememberSaveable { mutableStateOf(false) }
    var coverWidthPx by remember { mutableIntStateOf(0) }
    // Hoisted details-list state so the header can show the book title once its
    // titles block scrolls off screen. Only one host (portrait/landscape) is
    // composed at a time, so a single state serves both.
    val detailsListState = rememberLazyListState()
    // Shown the moment the titles block starts sliding behind the app bar:
    // it is the first visible item with a nonzero offset, or already past it.
    val showDetailsTitle =
        uiState.isGlance &&
            (
                detailsListState.firstVisibleItemIndex > BOOK_DETAILS_TITLE_ITEM_INDEX ||
                    (
                        detailsListState.firstVisibleItemIndex == BOOK_DETAILS_TITLE_ITEM_INDEX &&
                            detailsListState.firstVisibleItemScrollOffset > 0
                    )
            )
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Overall book progress (0..1): live position while playing, saved position while previewing.
    val bookProgress =
        run {
            val total = uiState.viewedTotalSeconds
            val position =
                if (uiState.isGlance) {
                    uiState.viewedProgressSeconds
                } else {
                    playbackState.currentBookPositionSeconds
                }
            if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
        }

    val chapterTitle =
        uiState.chapters
            .lastOrNull {
                it.startInSeconds <= playbackState.currentBookPositionSeconds
            }?.title
    val speedLabel = "${"%.2f".format(playbackState.playbackSpeed)}x"

    // Sheet dragging shares gestures with the details list through nested
    // scrolling: the list consumes what it can first, and only the leftover —
    // pulling down while parked at the top — moves the sheet. While the sheet
    // is mid-drag it consumes everything, like a bottom sheet. A fling the
    // sheet doesn't spend settling is handed back to the list.
    val sheetNestedScroll =
        remember(sheetDrag) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    if (sheetDrag.offsetPx.floatValue <= 0f) return Offset.Zero
                    return Offset(0f, sheetDrag.dragBy(available.y))
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y <= 0f) return Offset.Zero
                    return Offset(0f, sheetDrag.dragBy(available.y))
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (sheetDrag.offsetPx.floatValue <= 0f) return Velocity.Zero
                    sheetDrag.settle(available.y)
                    return if (sheetDrag.offsetPx.floatValue > 0f) available else Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    if (sheetDrag.offsetPx.floatValue <= 0f) return Velocity.Zero
                    sheetDrag.settle(available.y)
                    return if (sheetDrag.offsetPx.floatValue > 0f) available else Velocity.Zero
                }
            }
        }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (uiState.isGlance) {
                        // No drag detector here: it would race the chapter list
                        // and starve it of scroll gestures. Nested scrolling
                        // above carries sheet drags instead.
                        Modifier.nestedScroll(sheetNestedScroll)
                    } else {
                        // Playback has no scrollable content, so the sheet can
                        // own the drag detector directly.
                        Modifier.pointerInput(Unit) {
                            val tracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = { tracker.resetTracking() },
                                onDragEnd = {
                                    if (sheetDrag.offsetPx.floatValue > 0f) {
                                        sheetDrag.onDragEnd(tracker.calculateVelocity().y)
                                    }
                                },
                                onDragCancel = { sheetDrag.onDragCancel() },
                                onVerticalDrag = { change, dragDelta ->
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    sheetDrag.dragBy(dragDelta)
                                },
                            )
                        }
                    },
                ),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderRow(
                onMinimize = onMinimize,
                title = if (showDetailsTitle) audiobook.title else null,
            )

            if (isLandscape) {
                NowPlayingScreenLandscape(
                    modifier = Modifier.weight(1f),
                    audiobook = audiobook,
                    bookProgress = bookProgress,
                    isGlance = uiState.isGlance,
                    chapters = uiState.chapters,
                    inProgressSeconds = uiState.viewedProgressSeconds,
                    playbackState = playbackState,
                    chapterTitle = chapterTitle,
                    speedLabel = speedLabel,
                    onPlayViewed = viewModel::playViewedAudiobook,
                    onPlayChapter = viewModel::playChapter,
                    onSeekTo = viewModel::seekTo,
                    onSeekBackward = viewModel::seekBackward,
                    onSeekForward = viewModel::seekForward,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPreviousChapter = viewModel::previousChapter,
                    onNextChapter = viewModel::nextChapter,
                    onSpeedClick = { showSpeedSheet = true },
                    onChaptersClick = { showChaptersSheet = true },
                    detailsListState = detailsListState,
                )
            } else {
                if (uiState.isGlance) {
                    BookDetailsScreen(
                        modifier = Modifier.weight(1f),
                        audiobook = audiobook,
                        bookProgress = bookProgress,
                        chapters = uiState.chapters,
                        inProgressSeconds = uiState.viewedProgressSeconds,
                        onPlay = viewModel::playViewedAudiobook,
                        onPlayChapter = viewModel::playChapter,
                        listState = detailsListState,
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Title and Author
                        BookTitleBlock(audiobook = audiobook, textAlign = TextAlign.Center)
                    }

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
            if (!uiState.isGlance) {
                uiState.chapters.indexOfLast {
                    it.startInSeconds <= playbackState.currentBookPositionSeconds
                }
            } else {
                -1
            }
        ChaptersSheet(
            chapters = uiState.chapters,
            currentChapterIndex = currentChapterIndex,
            isPlaying = playbackState.isPlaying && !uiState.isGlance,
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
    val progress =
        if (playbackState.durationMs > 0) {
            playbackState.currentPositionMs.toFloat() / playbackState.durationMs
        } else {
            0f
        }

    if (!chapterTitle.isNullOrBlank()) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Slider(
        value = progress,
        onValueChange = { onSeekTo((it * playbackState.durationMs).toLong()) },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = playbackState.currentPositionMs.msToDurationLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = playbackState.durationMs.msToDurationLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousChapter,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.now_playing_previous_chapter),
                modifier = Modifier.size(32.dp),
            )
        }
        SeekButton(label = "15", onClick = onSeekBackward)
        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(96.dp),
        ) {
            Icon(
                imageVector =
                    if (playbackState.isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        }
        SeekButton(label = "30", onClick = onSeekForward)
        IconButton(
            onClick = onNextChapter,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.now_playing_next_chapter),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun HeaderRow(
    onMinimize: () -> Unit,
    title: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMinimize,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.close),
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedVisibility(
            visible = title != null,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    // Thin, read-only overall-book progress bar.
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
internal fun BookTitleBlock(
    audiobook: Audiobook,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = audiobook.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = audiobook.author,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
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
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.now_playing_chapters),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}
