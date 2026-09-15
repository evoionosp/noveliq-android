package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.utils.SheetDragState

/**
 * Landscape book preview: same 40/60 split as the landscape Now Playing, with
 * artwork + titles static on the left and the Continue action over a scrolling
 * chapters list on the right. Stateless — data and actions come from
 * NowPlayingViewModel, like the portrait details page.
 */
@Composable
internal fun BookDetailsLandscape(
    modifier: Modifier = Modifier,
    audiobook: Audiobook,
    bookProgress: Float,
    chapters: List<AudiobookChapter>,
    inProgressSeconds: Double,
    onPlay: () -> Unit,
    onPlayChapter: (AudiobookChapter) -> Unit,
    sheetDrag: SheetDragState,
) {
    var coverWidthPx by remember { mutableIntStateOf(0) }
    val inProgressIndex = inProgressChapterIndex(chapters, inProgressSeconds)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(0.4f)
                    // Static pane: nothing scrollable underneath, so the sheet
                    // can own vertical drags here directly. (List drags on the
                    // right arrive via nested scroll instead.)
                    .pointerInput(Unit) {
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
                    },
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
                LazyColumn(modifier = Modifier.weight(1f)) {
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
    }
}
