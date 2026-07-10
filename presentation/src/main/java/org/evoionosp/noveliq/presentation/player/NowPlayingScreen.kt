package org.evoionosp.noveliq.presentation.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingScreen(
    accessToken: String,
    onMinimize: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audiobook = uiState.viewedAudiobook ?: return
    val playbackState = uiState.playback
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var coverWidthPx by remember { mutableIntStateOf(0) }

    // Overall book progress (0..1): live position while playing, saved position while previewing.
    val bookProgress = run {
        val total = uiState.viewedTotalSeconds
        val position = if (uiState.isGlance) {
            uiState.viewedProgressSeconds
        } else {
            playbackState.currentBookPositionSeconds
        }
        if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAmount > 120f) {
                            onMinimize()
                        }
                        dragAmount = 0f
                    },
                    onVerticalDrag = { _, dragDelta ->
                        dragAmount += dragDelta
                    }
                )
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Book info block. Weighted so it fills the space above the controls; its content is
            // top-aligned, which pushes the flexible gap to sit between the info and the controls
            // (rather than leaving dead space at the bottom).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Cover Image
                AsyncImage(
                    model = authorizedImageRequest(
                        url = audiobook.coverUrl,
                        accessToken = accessToken
                    ),
                    contentDescription = audiobook.title,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .onSizeChanged { coverWidthPx = it.width }
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Thin, read-only overall-book progress bar, matched to the cover width.
                LinearProgressIndicator(
                    progress = { bookProgress },
                    modifier = Modifier
                        .width(with(LocalDensity.current) { coverWidthPx.toDp() })
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title and Author
                Text(
                    text = audiobook.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = audiobook.author,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (uiState.isGlance) {
                GlanceTransport(
                    text = glanceStatusText(
                        hasProgress = uiState.viewedHasProgress,
                        progressSeconds = uiState.viewedProgressSeconds,
                        totalSeconds = uiState.viewedTotalSeconds,
                        speed = playbackState.playbackSpeed
                    ),
                    onPlay = viewModel::playViewedAudiobook
                )
            } else {
                PlayingTransport(
                    playbackState = playbackState,
                    chapterTitle = uiState.chapters
                        .lastOrNull { it.startInSeconds <= playbackState.currentBookPositionSeconds }
                        ?.title,
                    onSeekTo = viewModel::seekTo,
                    onSeekBackward = viewModel::seekBackward,
                    onSeekForward = viewModel::seekForward,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPreviousChapter = viewModel::previousChapter,
                    onNextChapter = viewModel::nextChapter
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerAction(title = stringResource(R.string.now_playing_sleep), icon = "Zz")
                PlayerAction(
                    title = stringResource(R.string.now_playing_speed),
                    icon = "${"%.1f".format(playbackState.playbackSpeed)}x",
                    onClick = { showSpeedSheet = true }
                )
                Column(
                    modifier = Modifier.clickable { showChaptersSheet = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.now_playing_chapters),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showSpeedSheet) {
        SpeedSheet(
            speed = playbackState.playbackSpeed,
            onSpeedChange = viewModel::setPlaybackSpeed,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showChaptersSheet) {
        val currentChapterIndex = if (!uiState.isGlance) {
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
            onDismiss = { showChaptersSheet = false }
        )
    }
}

@Composable
private fun PlayingTransport(
    playbackState: PlaybackState,
    chapterTitle: String?,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val progress = if (playbackState.durationMs > 0) {
        playbackState.currentPositionMs.toFloat() / playbackState.durationMs
    } else 0f

    if (!chapterTitle.isNullOrBlank()) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Slider(
        value = progress,
        onValueChange = { onSeekTo((it * playbackState.durationMs).toLong()) },
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = playbackState.currentPositionMs.msToDurationLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = playbackState.durationMs.msToDurationLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousChapter,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.now_playing_previous_chapter),
                modifier = Modifier.size(32.dp)
            )
        }
        SeekButton(label = "15", onClick = onSeekBackward)
        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
        }
        SeekButton(label = "30", onClick = onSeekForward)
        IconButton(
            onClick = onNextChapter,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.now_playing_next_chapter),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun GlanceTransport(
    text: String,
    onPlay: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Larger (~2x) play button; no forward/backward transport in the glance state.
    FilledIconButton(
        onClick = onPlay,
        modifier = Modifier.size(144.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.now_playing_play),
            modifier = Modifier.size(96.dp)
        )
    }
}

@Composable
private fun glanceStatusText(
    hasProgress: Boolean,
    progressSeconds: Double,
    totalSeconds: Double,
    speed: Float
): String {
    val current = if (hasProgress) progressSeconds else 0.0
    val remainingContent = (totalSeconds - current).coerceAtLeast(0.0)
    val finishSeconds = if (speed > 0f) remainingContent / speed else remainingContent
    val timeLabel = finishSeconds.toHoursMinutesLabel()
    val speedLabel = "%.2fx".format(speed)
    val isDefaultSpeed = abs(speed - 1.0f) < 0.01f

    return when {
        hasProgress && isDefaultSpeed ->
            stringResource(R.string.now_playing_remaining, timeLabel)
        hasProgress ->
            stringResource(R.string.now_playing_remaining_at_speed, timeLabel, speedLabel)
        isDefaultSpeed ->
            timeLabel
        else ->
            stringResource(R.string.now_playing_duration_at_speed, timeLabel, speedLabel)
    }
}