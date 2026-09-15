package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R

/**
 * Index of the chapter containing [positionSeconds], or -1 when unknown. Works for
 * saved (details) and live (playing) positions alike — play state only animates the
 * marker, it never changes which row is current.
 */
internal fun inProgressChapterIndex(
    chapters: List<AudiobookChapter>,
    positionSeconds: Double,
): Int = chapters.indexOfLast { it.startInSeconds <= positionSeconds }

/**
 * Position of the titles block inside the details list. The parent watches
 * [LazyListState.firstVisibleItemIndex] against this to show the book title in
 * the app bar once the titles have scrolled off screen.
 */
internal const val BOOK_DETAILS_TITLE_ITEM_INDEX = 1

/**
 * Book-details view shown when the viewed book is not the playing one:
 * full-size cover, titles, a normal-sized play bar, and the embedded chapters
 * list. Stateless by design — all data and actions come from NowPlayingViewModel,
 * which already loads exactly this content. The whole page is a single
 * LazyColumn so it scrolls as one unit on short screens.
 *
 * @param listState hoisted so the host can react to scroll position (e.g. show
 * the book title in the app bar once [BOOK_DETAILS_TITLE_ITEM_INDEX] scrolls off).
 */
@Composable
internal fun BookDetailsScreen(
    modifier: Modifier = Modifier,
    audiobook: Audiobook,
    bookProgress: Float,
    chapters: List<AudiobookChapter>,
    inProgressSeconds: Double,
    onPlay: () -> Unit,
    onPlayChapter: (AudiobookChapter) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    var coverWidthPx by remember { mutableIntStateOf(0) }
    val inProgressIndex = inProgressChapterIndex(chapters, inProgressSeconds)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            BookCoverArtwork(
                audiobook = audiobook,
                modifier =
                    Modifier
                        .fillMaxWidth()
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
        }
        item {
            BookTitleBlock(
                audiobook = audiobook,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.now_playing_play),
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.now_playing_chapters),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (chapters.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.now_playing_chapters_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                ChapterRow(
                    chapter = chapter,
                    isCurrent = index == inProgressIndex,
                    // Viewed book is never the playing one here; nothing may animate.
                    isPlaying = false,
                    onPlay = { onPlayChapter(chapter) },
                )
            }
        }
    }
}
