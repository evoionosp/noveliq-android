package org.evoionosp.noveliq.presentation.utils

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dominant hue of the image at [request], or null until loaded (or when the
 * load fails, in which case callers simply render untinted). Sampled from a
 * thumbnail with a saturation lift so the wash reads as a hue rather than
 * gray. Dependency-free on purpose: the bitmap comes through the existing
 * Coil pipeline instead of a Palette fetch.
 */
@Composable
internal fun rememberDominantColor(request: ImageRequest): Color? {
    val context = LocalContext.current
    var dominant by remember(request.data) { mutableStateOf<Color?>(null) }
    LaunchedEffect(request) {
        dominant =
            withContext(Dispatchers.Default) {
                try {
                    val result =
                        Coil.imageLoader(context).execute(
                            request.newBuilder().allowHardware(false).build(),
                        )
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let(::sampleDominant)
                } catch (_: Exception) {
                    null
                }
            }
    }
    return dominant
}

/**
 * Same hue, reduced brightness — for a "finished portion" shade of [this].
 */
internal fun Color.darker(factor: Float = 0.65f): Color {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv,
    )
    hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}

/** Opaque linear mix of two colors — [ratio] 0 is [this], 1 is [other]. */
internal fun Color.mixedWith(
    other: Color,
    ratio: Float,
): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red * (1 - r) + other.red * r,
        green * (1 - r) + other.green * r,
        blue * (1 - r) + other.blue * r,
        alpha * (1 - r) + other.alpha * r,
    )
}

private fun sampleDominant(bitmap: Bitmap): Color? {
    if (bitmap.isRecycled) return null
    val thumb = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var count = 0
    for (x in 0 until thumb.width) {
        for (y in 0 until thumb.height) {
            val pixel = thumb.getPixel(x, y)
            if (AndroidColor.alpha(pixel) < 128) continue
            redSum += AndroidColor.red(pixel)
            greenSum += AndroidColor.green(pixel)
            blueSum += AndroidColor.blue(pixel)
            count++
        }
    }
    if (thumb !== bitmap) thumb.recycle()
    if (count == 0) return null
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (redSum / count).toInt(),
        (greenSum / count).toInt(),
        (blueSum / count).toInt(),
        hsv,
    )
    hsv[1] = (hsv[1] * 1.4f + 0.08f).coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}
