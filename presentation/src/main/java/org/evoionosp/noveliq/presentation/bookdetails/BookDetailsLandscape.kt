package org.evoionosp.noveliq.presentation.bookdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.player.BookCoverArtwork
import org.evoionosp.noveliq.presentation.player.BookProgressBar
import org.evoionosp.noveliq.presentation.player.BookTitleBlock
import org.evoionosp.noveliq.presentation.player.ChapterRow
import org.evoionosp.noveliq.presentation.player.inProgressChapterIndex

/**
 * Landscape book details: same 40/60 split as the landscape Now Playing,
 * with artwork + titles static on the left and the Continue action over a
 * scrolling chapters list on the right. Hosted by the standalone details
 * route — no sheet behavior. Stateless: data and actions come from the caller.
 */
@Composable
internal fun BookDetailsLandscape(
    modifier: Modifier = Modifier,
    audiobook: Audiobook,
    bookProgress: Float,
    chapters: List<AudiobookChapter>,
    inProgressSeconds: Double,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPlayChapter: (AudiobookChapter) -> Unit,
    miniTrailPadding: Dp = 0.dp,
) {
    var coverWidthPx by remember { mutableIntStateOf(0) }
    val inProgressIndex = inProgressChapterIndex(chapters, inProgressSeconds)

    Row(
        modifier = modifier.fillMaxWidth(),
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
            modifier =
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
        ) {
            ContinueListeningButton(onPlay = onPlay)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.now_playing_chapters),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (chapters.isEmpty()) {
                Text(
                    text = stringResource(R.string.now_playing_chapters_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = miniTrailPadding),
                ) {
                    items(chapters.size) { index ->
                        val chapter = chapters[index]
                        ChapterRow(
                            chapter = chapter,
                            isCurrent = index == inProgressIndex,
                            isPlaying = isPlaying && index == inProgressIndex,
                            onPlay = { onPlayChapter(chapter) },
                        )
                    }
                }
            }
        }
    }
}
