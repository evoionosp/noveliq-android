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
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    return when {
        hours > 0 -> "%d:%02d:00".format(hours, minutes)
        else -> "%d:%02d".format(minutes, 0)
    }
}
