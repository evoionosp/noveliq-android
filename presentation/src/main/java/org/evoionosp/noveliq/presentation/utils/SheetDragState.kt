package org.evoionosp.noveliq.presentation.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Drag distance (as a fraction of travel) that alone decides the settle target. */
const val SHEET_SETTLE_DRAG_FRACTION = 0.15f

/** Fling velocity that alone decides the settle target. Positive Y is downward. */
const val SHEET_FLING_VELOCITY_PX_PER_S = 800f

/** Rubber-band overshoot allowed past the expanded anchor, as a fraction of travel. */
private const val SHEET_OVERSHOOT_FRACTION = 0.08f
private const val SHEET_ANIM_MILLIS = 255

enum class SheetTarget {
    EXPANDED,
    COLLAPSED,
}

/**
 * Pure settle policy for the Now Playing sheet. Fractions are 0 (collapsed,
 * mini bar) to 1 (fully expanded); velocity is in px/s with positive Y pointing
 * down. A decisive drag wins first, then a decisive fling, otherwise the
 * nearest anchor wins. Kept free of Compose so it is unit-testable.
 */
fun resolveSheetTarget(
    startFraction: Float,
    endFraction: Float,
    velocityYPxPerS: Float,
): SheetTarget {
    val dragged = endFraction - startFraction
    if (abs(dragged) > SHEET_SETTLE_DRAG_FRACTION) {
        return if (dragged > 0f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
    }
    if (abs(velocityYPxPerS) > SHEET_FLING_VELOCITY_PX_PER_S) {
        return if (velocityYPxPerS < 0f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
    }
    return if (endFraction > 0.5f) SheetTarget.EXPANDED else SheetTarget.COLLAPSED
}

/**
 * Bidirectional drag state for the Now Playing sheet. One state is hoisted
 * above both the mini bar and the full-screen sheet so a single finger drag
 * can travel between them: dragging up from the mini bar expands the sheet,
 * dragging down from the full sheet collapses it.
 *
 * Position is [offsetPx]: 0 is fully expanded, [travelPx] is fully collapsed
 * (the sheet sits exactly off screen, where the mini bar lives). A small
 * overshoot past the expanded anchor is allowed mid-drag for a rubber-band
 * feel and removed on settle; the collapsed anchor is a hard stop, so the
 * mini bar can never be dragged down over the content below it.
 *
 * Drag updates are synchronous; only settle/programmatic animations run in
 * [scope].
 */
@Stable
class SheetDragState(
    private val scope: CoroutineScope,
    private val onCollapsed: () -> Unit,
    private val onExpanded: () -> Unit,
) {
    val offsetPx = mutableFloatStateOf(0f)

    /** True once the overlay has reported its height; animations wait for this. */
    var travelKnown by mutableStateOf(false)
        private set

    var travelPx by mutableFloatStateOf(0f)
        private set

    /** 0 (collapsed) to 1 (expanded), derived from the current offset. */
    val expansionFraction: Float
        get() {
            val travel = travelPx
            if (travel <= 0f) return 0f
            return (1f - offsetPx.floatValue / travel).coerceIn(0f, 1f)
        }

    private var settleJob: Job? = null
    private var dragging = false
    private var dragStartOffsetPx = 0f

    /**
     * Called by the overlay once its height is known. The first call always
     * snaps to the collapsed anchor: a fresh state rests at offset 0 (the
     * expanded position), so without this the sheet would flash open. Hosts
     * animate to expanded afterwards when their target asks for it, which
     * replays the rise-in animation after rotation too.
     */
    fun onTravelKnown(heightPx: Float) {
        if (heightPx <= 0f) return
        if (!travelKnown) {
            travelKnown = true
            travelPx = heightPx
            settleJob?.cancel()
            dragging = false
            offsetPx.floatValue = heightPx
        } else {
            travelPx = heightPx
        }
    }

    fun dragBy(deltaPx: Float): Float {
        if (deltaPx == 0f) return 0f
        val travel = travelPx
        if (travel <= 0f) return 0f
        settleJob?.cancel()
        if (!dragging) {
            dragging = true
            dragStartOffsetPx = offsetPx.floatValue
        }
        val old = offsetPx.floatValue
        // Hard stop at collapsed: the mini bar never moves down. Top
        // overshoot stays for the expanded rubber-band feel.
        val new =
            (old + deltaPx).coerceIn(
                -travel * SHEET_OVERSHOOT_FRACTION,
                travel,
            )
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
        dragging = false
        val travel = travelPx
        if (travel <= 0f) return
        val startFraction = (1f - dragStartOffsetPx / travel).coerceIn(0f, 1f)
        val endFraction = (1f - offsetPx.floatValue / travel).coerceIn(0f, 1f)
        when (resolveSheetTarget(startFraction, endFraction, velocityYPxPerS)) {
            SheetTarget.EXPANDED -> {
                animateOffsetTo(0f)
                onExpanded()
            }

            SheetTarget.COLLAPSED -> {
                animateOffsetTo(travel)
                onCollapsed()
            }
        }
    }

    /** Tap-to-expand (mini bar) and reopen paths. No-op until travel is known. */
    fun expand() {
        settleJob?.cancel()
        if (travelPx <= 0f) return
        dragging = false
        settleJob = scope.launch { animateOffsetTo(0f) }
    }

    /** Back/minimize path. Idempotent with the target flag held by the host. */
    fun collapse() {
        settleJob?.cancel()
        if (travelPx <= 0f) return
        dragging = false
        settleJob =
            scope.launch {
                animateOffsetTo(travelPx)
                onCollapsed()
            }
    }

    private suspend fun animateOffsetTo(target: Float) {
        if (offsetPx.floatValue == target) return
        Animatable(offsetPx.floatValue).animateTo(
            targetValue = target,
            animationSpec = tween(SHEET_ANIM_MILLIS, easing = FastOutSlowInEasing),
        ) {
            offsetPx.floatValue = value
        }
    }
}
