package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.utils.BookCoverArtwork
import org.evoionosp.noveliq.presentation.utils.CoverArt
import org.evoionosp.noveliq.presentation.utils.darker
import org.evoionosp.noveliq.presentation.utils.mixedWith
import org.evoionosp.noveliq.presentation.utils.rememberDominantColor

@Composable
fun NowPlayingBar(
    audiobook: Audiobook?,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    if (audiobook == null) return
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Finished portion of the current chapter (0..1): chapter end falls back
    // to the next chapter's start, then to the book total.
    val chapters = uiState.chapters
    val positionSeconds = playbackState.currentBookPositionSeconds
    val chapterIndex = inProgressChapterIndex(chapters, positionSeconds)
    val chapterProgress =
        chapters.getOrNull(chapterIndex)?.let { chapter ->
            val endSeconds =
                chapter.endInSeconds?.toDouble()
                    ?: chapters.getOrNull(chapterIndex + 1)?.startInSeconds?.toDouble()
                    ?: uiState.viewedTotalSeconds.takeIf { it > 0 }
                    ?: return@let 0f
            if (endSeconds <= chapter.startInSeconds) return@let 0f
            ((positionSeconds - chapter.startInSeconds) / (endSeconds - chapter.startInSeconds))
                .toFloat()
                .coerceIn(0f, 1f)
        } ?: 0f
    // The pipeline always resolves art — real cover or generated hardcover —
    // so sampling always yields a tint.
    val dominantColor =
        rememberDominantColor(
            authorizedImageRequest(
                CoverArt(
                    url = audiobook.coverUrl,
                    title = audiobook.title,
                    author = audiobook.author,
                ),
            ),
        )
    // Two-tone bar, blended opaquely so coverage is exact by construction:
    // base wash over the whole bar, finished portion a deeper shade of it.
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val barColor =
        if (dominantColor != null) surfaceColor.mixedWith(dominantColor, BASE_BLEND) else surfaceColor
    val progressColor =
        if (dominantColor != null) {
            barColor.mixedWith(dominantColor.darker(), PROGRESS_BLEND)
        } else {
            barColor
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(onClick = onExpand),
        shape = RoundedCornerShape(22.dp),
        // Zero so the blended hue stays honest instead of picking up the
        // primary tonal overlay.
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        color = barColor,
    ) {
        Box {
            if (dominantColor != null && chapterProgress > 0f) {
                ProgressFill(
                    fraction = chapterProgress,
                    color = progressColor,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BookCoverArtwork(
                    coverUrl = audiobook.coverUrl,
                    title = audiobook.title,
                    author = audiobook.author,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = audiobook.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = audiobook.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = viewModel::togglePlayPause) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private const val BASE_BLEND = 0.32f
private const val PROGRESS_BLEND = 0.6f

/**
 * Fills [fraction] of its parent's width at the parent's full height.
 * Measures from the parent's final size, so — unlike a fractional modifier
 * under tight constraints — the width can never clamp back to full, and the
 * height can never fall short.
 */
@Composable
private fun ProgressFill(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = { Box(modifier = Modifier.background(color)) },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = (constraints.maxWidth * fraction).roundToInt().coerceIn(0, constraints.maxWidth)
        val placeable =
            measurables.single().measure(Constraints.fixed(width, constraints.maxHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(0, 0)
        }
    }
}
