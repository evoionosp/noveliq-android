package org.evoionosp.noveliq.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.utils.SheetDragState

@Composable
fun NowPlayingOverlay(
    visible: Boolean,
    audiobook: Audiobook?,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = visible) {
        onMinimize()
    }

    // Hoisted transition state (instead of a plain Boolean) so the enter
    // animation also plays when first composed already-visible — e.g. Activity
    // recreation with the expanded player restored.
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible && audiobook != null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        // Latest-callback holder: the expand/minimize lambdas are recreated by
        // the parent on every recomposition.
        val currentOnMinimize by rememberUpdatedState(onMinimize)

        AnimatedVisibility(
            visibleState = transitionState,
            enter =
                slideInVertically(
                    animationSpec = tween(durationMillis = 350),
                    initialOffsetY = { fullHeight -> fullHeight },
                ) + fadeIn(animationSpec = tween(200)),
            exit =
                slideOutVertically(
                    animationSpec = tween(durationMillis = 300),
                    targetOffsetY = { fullHeight -> fullHeight },
                ) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 300),
                        targetScale = 0.96f,
                        // Sink toward the mini bar the sheet collapses into.
                        transformOrigin = TransformOrigin(0.5f, 1f),
                    ) +
                    fadeOut(animationSpec = tween(150)),
        ) {
            if (audiobook != null) {
                // Remember the drag state inside the animated content so it is
                // forgotten when the sheet closes. Otherwise a swipe-dismiss
                // would leave the next open translated off screen.
                val sheetScope = rememberCoroutineScope()
                val sheetDrag =
                    remember(maxHeightPx, audiobook) {
                        SheetDragState(maxHeightPx, sheetScope) { currentOnMinimize() }
                    }

                // Draw-phase translation: follows the finger with no relayout.
                // When a drag dismisses the sheet it is already off screen, so
                // the exit transition below plays out invisibly — no flash.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = sheetDrag.offsetPx.floatValue },
                ) {
                    NowPlayingScreen(
                        onMinimize = onMinimize,
                        sheetDrag = sheetDrag,
                    )
                }
            }
        }
    }
}
