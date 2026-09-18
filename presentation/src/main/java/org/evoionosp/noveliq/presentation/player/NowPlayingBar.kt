package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.utils.BookCoverArtwork

/**
 * Mini player content: circular artwork, marquee titles, and prev / play-pause
 * / next transport. Styling- and gesture-free on purpose — the unified sheet
 * provides the card surface, the drag handling, and the tap action, so this
 * renders identically at the collapsed end of the morph.
 */
@Composable
internal fun MiniPlayerContent(
    audiobook: Audiobook,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    fun tick() = hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val pc = LocalPlayerColors.current

    // Current chapter, falling back to the book when chapters are unknown.
    val chapterTitle =
        uiState.chapters
            .getOrNull(
                inProgressChapterIndex(uiState.chapters, playbackState.currentBookPositionSeconds),
            )?.title
            ?: audiobook.title

    Row(
        modifier =
            modifier
                .height(64.dp)
                .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCoverArtwork(
            coverUrl = audiobook.coverUrl,
            title = audiobook.title,
            author = audiobook.author,
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(MINI_CORNER_RADIUS)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            MarqueeText(
                text = chapterTitle,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = pc.textPrimary,
                    ),
            )
            MarqueeText(
                text = "${audiobook.title} - ${audiobook.author}",
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = pc.textSecondary,
                    ),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        MiniSeekButton(
            label = "15",
            onClick = {
                tick()
                viewModel.seekBackward()
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        MorphingPlayPauseButton(
            isPlaying = playbackState.isPlaying,
            onClick = {
                tick()
                viewModel.togglePlayPause()
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        MiniSeekButton(
            label = "30",
            onClick = {
                tick()
                viewModel.seekForward()
            },
        )
    }
}

@Composable
private fun MiniSeekButton(
    label: String,
    onClick: () -> Unit,
) {
    val pc = LocalPlayerColors.current
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(pc.containerSubtle)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = pc.accent,
        )
    }
}

/**
 * Single-line text that scrolls when it overflows, with both edges faded so
 * the loop point never hard-clips mid-glyph.
 */
@Composable
private fun MarqueeText(
    text: String,
    style: TextStyle,
) {
    Text(
        text = text,
        style = style,
        maxLines = 1,
        modifier =
            Modifier
                .basicMarquee(iterations = Int.MAX_VALUE)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.92f to Color.Black,
                                1f to Color.Transparent,
                            ),
                        blendMode = BlendMode.DstIn,
                    )
                },
    )
}
