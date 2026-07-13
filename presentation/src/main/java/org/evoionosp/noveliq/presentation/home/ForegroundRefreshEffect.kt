package org.evoionosp.noveliq.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Invokes [onForeground] every time the screen returns to the foreground (each ON_START
 * after the first). The very first ON_START is skipped because the initial content is
 * already loaded during startup bootstrap / from the local cache, so refreshing again
 * immediately would be redundant.
 */
@Composable
fun ForegroundRefreshEffect(onForeground: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnForeground by rememberUpdatedState(onForeground)

    DisposableEffect(lifecycleOwner) {
        var skippedInitialStart = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (skippedInitialStart) {
                    currentOnForeground()
                } else {
                    skippedInitialStart = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
