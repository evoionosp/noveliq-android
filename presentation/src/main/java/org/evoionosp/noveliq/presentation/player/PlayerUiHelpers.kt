package org.evoionosp.noveliq.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
internal fun authorizedImageRequest(
    url: String,
    accessToken: String
): ImageRequest {
    return ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(true)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .addHeader("Authorization", "Bearer $accessToken")
        .build()
}

internal fun Long.toDurationLabel(): String {
    val totalSeconds = this
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

internal fun Long.msToDurationLabel(): String = (this / 1000).toDurationLabel()
