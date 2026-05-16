package org.evoionosp.noveliq.presentation.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook

@Composable
fun NowPlayingOverlay(
    visible: Boolean,
    audiobook: Audiobook?,
    accessToken: String,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && audiobook != null,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 320),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 280),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(120)),
        modifier = modifier
    ) {
        if (audiobook != null) {
            NowPlayingScreen(
                audiobook = audiobook,
                accessToken = accessToken,
                onMinimize = onMinimize
            )
        }
    }
}
