package org.evoionosp.noveliq.presentation.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DISMISS_FRACTION = 0.25f
private const val DISMISS_VELOCITY_PX_PER_S = 800f
private const val OFF_SCREEN_ANIM_MILLIS = 220

/**
 * Interactive drag state for the Now Playing sheet. The sheet follows the
 * finger via [dragBy] (clamped to downward movement, returns the consumed
 * delta) and [settle] either springs back or finishes sliding off screen
 * based on dragged distance and fling velocity, invoking [onDismissed] in the
 * latter case.
 *
 * Drag updates are synchronous so nested-scroll handlers can report exactly
 * what the sheet consumed; only the release animation runs in [scope]. Plain
 * state holder — the host owns the gesture detectors and decides when a drag
 * belongs to the sheet versus inner scrollable content.
 */
@Stable
internal class SheetDragState(
    private val maxHeightPx: Float,
    private val scope: CoroutineScope,
    private val onDismissed: () -> Unit,
) {
    val offsetPx = mutableFloatStateOf(0f)
    private var settleJob: Job? = null

    fun dragBy(deltaPx: Float): Float {
        if (deltaPx == 0f) return 0f
        settleJob?.cancel()
        val old = offsetPx.floatValue
        val new = (old + deltaPx).coerceIn(0f, maxHeightPx)
        offsetPx.floatValue = new
        return new - old
    }

    fun onDragEnd(velocityYPxPerS: Float) {
        settleJob?.cancel()
        settleJob = scope.launch { settle(velocityYPxPerS) }
    }

    fun onDragCancel() {
        settleJob?.cancel()
        settleJob = scope.launch { settle(0f) }
    }

    suspend fun settle(velocityYPxPerS: Float) {
        val target =
            if (offsetPx.floatValue > maxHeightPx * DISMISS_FRACTION ||
                velocityYPxPerS > DISMISS_VELOCITY_PX_PER_S
            ) {
                maxHeightPx
            } else {
                0f
            }
        Animatable(offsetPx.floatValue).animateTo(
            targetValue = target,
            animationSpec = if (target == 0f) spring() else tween(OFF_SCREEN_ANIM_MILLIS),
        ) {
            offsetPx.floatValue = value
        }
        if (target == maxHeightPx) {
            onDismissed()
        }
    }
}
