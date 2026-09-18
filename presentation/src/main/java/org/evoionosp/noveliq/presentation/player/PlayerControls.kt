package org.evoionosp.noveliq.presentation.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun SeekButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pc = LocalPlayerColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = pc.containerSubtle,
        // Explicit: a raw mixed tone has no scheme content color.
        contentColor = pc.accent,
        modifier = modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun PlayerAction(
    title: String,
    icon: String,
    onClick: (() -> Unit)? = null, // Add optional click
) {
    val pc = LocalPlayerColors.current
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = pc.textPrimary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = pc.textSecondary,
        )
    }
}

/**
 * Corner of the play/pause container as a fraction of its size: round while
 * paused (play triangle showing), squarer while playing (pause bars
 * showing). 0.41 / 0.23 reproduce the original mini-bar 18dp / 10dp
 * corners at 44dp.
 */
private const val PLAY_PAUSE_PAUSED_CORNER_FRACTION = 0.41f
private const val PLAY_PAUSE_PLAYING_CORNER_FRACTION = 0.23f

/**
 * Animated play/pause container corner for [buttonSize]. Shared by every
 * play button so the round-paused / square-playing morph looks identical
 * at any size.
 */
@Composable
internal fun animatePlayPauseCorner(
    isPlaying: Boolean,
    buttonSize: Dp,
): State<Dp> =
    animateDpAsState(
        targetValue =
            if (isPlaying) {
                buttonSize * PLAY_PAUSE_PLAYING_CORNER_FRACTION
            } else {
                buttonSize * PLAY_PAUSE_PAUSED_CORNER_FRACTION
            },
        label = "PlayPauseCorner",
    )

/**
 * Play/pause glyph that crossfades/scales instead of swapping abruptly. A
 * lightweight stand-in for a true path-morphing icon, with no extra
 * dependencies.
 */
@Composable
internal fun PlayPauseGlyph(
    isPlaying: Boolean,
    tint: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    playContentDescription: String? = null,
    pauseContentDescription: String? = null,
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith
                (fadeOut() + scaleOut(targetScale = 0.6f))
        },
        label = "PlayPauseIcon",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) pauseContentDescription else playContentDescription,
            tint = tint,
            modifier = modifier.size(iconSize),
        )
    }
}

/**
 * Play/pause button whose container corner animates with the state. Round
 * while paused, squarer while playing; the glyph crossfades to match.
 * Defaults reproduce the mini-bar button; larger transports pass their size.
 */
@Composable
internal fun MorphingPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 28.dp,
) {
    val pc = LocalPlayerColors.current
    val corner by animatePlayPauseCorner(isPlaying, buttonSize)
    Box(
        modifier =
            modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(corner))
                .background(pc.accent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PlayPauseGlyph(
            isPlaying = isPlaying,
            tint = pc.onAccent,
            iconSize = iconSize,
        )
    }
}
