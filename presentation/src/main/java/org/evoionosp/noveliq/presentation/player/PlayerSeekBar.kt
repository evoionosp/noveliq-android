package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Post-seek latch: after a commit, the thumb holds the committed fraction
 * until the player reports back within [SEEK_LATCH_TOLERANCE_FRACTION], or
 * [SEEK_LATCH_TIMEOUT_MS] passes and the seek is assumed lost. Same values
 * as PixelPlayerOSS's progress section.
 */
internal const val SEEK_LATCH_TIMEOUT_MS = 5_000L
internal const val SEEK_LATCH_TOLERANCE_FRACTION = 0.04f

/**
 * Playback progress as a 0..1 fraction. Unknown durations (0 or negative)
 * report 0 rather than dividing.
 */
internal fun progressFraction(
    positionMs: Long,
    durationMs: Long,
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

/**
 * Whether a latched seek target can be released: the player caught up
 * ([diffFraction] within tolerance) or the latch went stale ([elapsedMs]
 * past the timeout).
 */
internal fun shouldClearSeekTarget(
    elapsedMs: Long,
    diffFraction: Float,
): Boolean = elapsedMs > SEEK_LATCH_TIMEOUT_MS || diffFraction < SEEK_LATCH_TOLERANCE_FRACTION

/**
 * Chapter progress: PixelPlayerOSS-shaped wavy scrub slider with quantized
 * drag haptics and seek-on-release, plus SemiBold time labels beneath it.
 * While dragging, the labels preview the scrub target; after release, the
 * committed fraction latches until the player catches up, so the thumb
 * never snaps back to stale progress.
 */
@Composable
internal fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pc = LocalPlayerColors.current
    val haptics = LocalHapticFeedback.current
    val progress = progressFraction(positionMs, durationMs)

    var dragValue by remember { mutableStateOf<Float?>(null) }
    var seekTarget by remember { mutableFloatStateOf(-1f) }
    var seekFinishedAt by remember { mutableLongStateOf(0L) }
    val lastHapticStep = remember { intArrayOf(-1) }

    // New chapter (new duration): drop any in-flight scrub or latch so a
    // stale fraction from the previous chapter can't stick.
    LaunchedEffect(durationMs) {
        dragValue = null
        seekTarget = -1f
        seekFinishedAt = 0L
    }

    LaunchedEffect(progress) {
        if (dragValue != null) return@LaunchedEffect
        val target = seekTarget
        if (target < 0f) return@LaunchedEffect
        if (
            shouldClearSeekTarget(
                System.currentTimeMillis() - seekFinishedAt,
                abs(progress - target),
            )
        ) {
            seekTarget = -1f
        }
    }

    val displayFraction = dragValue ?: if (seekTarget >= 0f) seekTarget else progress
    // State-backed provider: WavySliderExpressive tracks this through
    // derivedStateOf, which would never refresh on a bare captured value.
    val displayState = rememberUpdatedState(displayFraction)

    val displayedMs =
        (displayFraction * durationMs).roundToLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    // Second-truncated like PixelPlayerOSS: labels tick once per second.
    val positionLabel = ((displayedMs / 1000) * 1000).msToDurationLabel()
    val durationLabel = durationMs.coerceAtLeast(0L).msToDurationLabel()

    Column(modifier = modifier.fillMaxWidth()) {
        WavySliderExpressive(
            value = { displayState.value },
            onValueChange = { newFraction ->
                dragValue = newFraction
                val quantized = (newFraction.coerceIn(0f, 1f) * 20f).toInt()
                if (quantized != lastHapticStep[0]) {
                    lastHapticStep[0] = quantized
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueCommit = { finalFraction ->
                seekTarget = finalFraction
                seekFinishedAt = System.currentTimeMillis()
                dragValue = null
                onSeekTo(
                    (finalFraction * durationMs).roundToLong().coerceIn(0L, durationMs.coerceAtLeast(0L)),
                )
            },
            activeTrackColor = pc.accent,
            inactiveTrackColor = pc.textPrimary.copy(alpha = 0.2f),
            thumbColor = pc.accent,
            isPlaying = isPlaying,
            trackEdgePadding = 0.dp,
            semanticsLabel = "Playback position",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = pc.textPrimary,
            )
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = pc.textPrimary,
            )
        }
    }
}
