package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.playback.PlaybackState

/**
 * Landscape Now Playing: 40/60 split with artwork + titles on the left and
 * transport + actions on the right. Everything is sized to fit without
 * scrolling — the cover shrinks by weight instead of overflowing, and the
 * panes center vertically in the available height. The glance state reuses
 * the shared [BookDetailsScreen] full-width instead.
 */
@Composable
internal fun NowPlayingScreenLandscape(
    modifier: Modifier = Modifier,
    audiobook: Audiobook,
    bookProgress: Float,
    isGlance: Boolean,
    chapters: List<AudiobookChapter>,
    inProgressSeconds: Double,
    playbackState: PlaybackState,
    chapterTitle: String?,
    speedLabel: String,
    onPlayViewed: () -> Unit,
    onPlayChapter: (AudiobookChapter) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSpeedClick: () -> Unit,
    onChaptersClick: () -> Unit,
    detailsListState: LazyListState,
) {
    var coverWidthPx by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isGlance) {
            BookDetailsScreen(
                modifier = Modifier.weight(1f),
                audiobook = audiobook,
                bookProgress = bookProgress,
                chapters = chapters,
                inProgressSeconds = inProgressSeconds,
                onPlay = onPlayViewed,
                onPlayChapter = onPlayChapter,
                listState = detailsListState,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                    BookProgressBar(
                        progress = bookProgress,
                        modifier =
                            Modifier
                                .width(with(LocalDensity.current) { coverWidthPx.toDp() })
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BookTitleBlock(audiobook = audiobook, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(
                    modifier = Modifier.weight(0.6f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayingTransport(
                        playbackState = playbackState,
                        chapterTitle = chapterTitle,
                        onSeekTo = onSeekTo,
                        onSeekBackward = onSeekBackward,
                        onSeekForward = onSeekForward,
                        onTogglePlayPause = onTogglePlayPause,
                        onPreviousChapter = onPreviousChapter,
                        onNextChapter = onNextChapter,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FooterActionsRow(
                        speedLabel = speedLabel,
                        onSpeedClick = onSpeedClick,
                        onChaptersClick = onChaptersClick,
                    )
                }
            }
        }
    }
}
