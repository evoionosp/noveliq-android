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
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decode size for tint sampling: an 8px average needs no more pixels. */
private const val SAMPLE_DECODE_PX = 64

/**
 * Process-lifetime art-palette cache, keyed by the request data (a [CoverArt]
 * value). Sampling runs once per book per process; every later composition
 * — same book revisited, sheet re-entered — reads the cached palette with no
 * Coil execute at all. Entries are tiny and bounded by library size, so no
 * eviction. Failures are never cached, so a later composition retries.
 */
private val artPaletteCache = ConcurrentHashMap<Any, ArtPalette>()

/**
 * One sampling result: [dominant] warms the background wash, [vibrant] seeds
 * the player accent scheme. Both come from the same decode.
 */
internal data class ArtPalette(
    val dominant: Color,
    val vibrant: Color,
)

/**
 * Art palette of the image at [request], or null until loaded (or when the
 * load fails, in which case callers simply render untinted). The dominant
 * average carries a saturation lift so the wash reads as a hue rather than
 * gray; the vibrant seed is the most saturated hue family, scored
 * PixelPlayer-style by chroma mass. Dependency-free on purpose: the bitmap
 * comes through the existing Coil pipeline instead of a Palette fetch. The
 * result is cached per book for the process lifetime, so repeats never
 * touch Coil.
 */
@Composable
internal fun rememberArtPalette(request: ImageRequest): ArtPalette? {
    val context = LocalContext.current
    // Stable data (a CoverArt value), not the request object: callers
    // rebuild ImageRequest every recomposition, and re-keying on it would
    // cancel the sample on every frame mid-morph and flash the tint.
    val key = request.data
    var palette by remember(key) { mutableStateOf(artPaletteCache[key]) }
    LaunchedEffect(key) {
        // Cache hit: nothing to do, not even a dispatcher hop.
        if (palette != null) return@LaunchedEffect
        val sampled =
            withContext(Dispatchers.Default) {
                try {
                    val result =
                        Coil.imageLoader(context).execute(
                            request
                                .newBuilder()
                                .allowHardware(false)
                                .size(SAMPLE_DECODE_PX, SAMPLE_DECODE_PX)
                                .build(),
                        )
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let(::sampleArtPalette)
                } catch (_: Exception) {
                    null
                }
            }
        if (sampled != null) artPaletteCache[key] = sampled
        palette = sampled
    }
    return palette
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

private fun sampleArtPalette(bitmap: Bitmap): ArtPalette? {
    if (bitmap.isRecycled) return null
    val thumb = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
    val pixels = IntArray(thumb.width * thumb.height)
    thumb.getPixels(pixels, 0, thumb.width, 0, 0, thumb.width, thumb.height)
    if (thumb !== bitmap) thumb.recycle()
    val opaque = pixels.filter { AndroidColor.alpha(it) >= 128 }.toIntArray()
    if (opaque.isEmpty()) return null
    // Dominant average — formula untouched, so the approved background tint
    // does not shift.
    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    for (pixel in opaque) {
        redSum += AndroidColor.red(pixel)
        greenSum += AndroidColor.green(pixel)
        blueSum += AndroidColor.blue(pixel)
    }
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (redSum / opaque.size).toInt(),
        (greenSum / opaque.size).toInt(),
        (blueSum / opaque.size).toInt(),
        hsv,
    )
    hsv[1] = (hsv[1] * 1.4f + 0.08f).coerceIn(0f, 1f)
    return ArtPalette(
        dominant = Color(AndroidColor.HSVToColor(hsv)),
        vibrant = Color(scoreVibrantSeed(opaque)),
    )
}

/** Hue buckets for the vibrancy vote; the winner is the most saturated family. */
private const val VIBRANT_HUE_BUCKETS = 12

/** Mean saturation below which art counts as effectively gray (no family earns the accent). */
private const val MIN_VIBRANT_MEAN_SATURATION = 0.10

/**
 * Most saturated hue family in [pixels] (non-empty, opaque packed ARGB).
 * Each pixel votes its saturation into its hue bucket, so the winner
 * balances punch (chroma) against presence (population); effectively gray
 * art falls back to the straight average. Pure Kotlin (no android.graphics)
 * so it stays unit-testable.
 */
internal fun scoreVibrantSeed(pixels: IntArray): Int {
    require(pixels.isNotEmpty())
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    val satMass = DoubleArray(VIBRANT_HUE_BUCKETS)
    val bucketR = LongArray(VIBRANT_HUE_BUCKETS)
    val bucketG = LongArray(VIBRANT_HUE_BUCKETS)
    val bucketB = LongArray(VIBRANT_HUE_BUCKETS)
    val bucketN = IntArray(VIBRANT_HUE_BUCKETS)
    for (argb in pixels) {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        rSum += r
        gSum += g
        bSum += b
        val hsv = rgbToHsv(r, g, b)
        val bucket = ((hsv[0] / 360f * VIBRANT_HUE_BUCKETS).toInt()) % VIBRANT_HUE_BUCKETS
        satMass[bucket] += hsv[1].toDouble()
        bucketR[bucket] += r
        bucketG[bucket] += g
        bucketB[bucket] += b
        bucketN[bucket] += 1
    }
    val n = pixels.size
    val avg = (0xFF shl 24) or ((rSum / n).toInt() shl 16) or ((gSum / n).toInt() shl 8) or (bSum / n).toInt()
    var best = 0
    for (i in 1 until VIBRANT_HUE_BUCKETS) {
        if (satMass[i] > satMass[best]) best = i
    }
    if (bucketN[best] == 0 || satMass[best] < n * MIN_VIBRANT_MEAN_SATURATION) return avg
    return (0xFF shl 24) or
        ((bucketR[best] / bucketN[best]).toInt() shl 16) or
        ((bucketG[best] / bucketN[best]).toInt() shl 8) or
        (bucketB[best] / bucketN[best]).toInt()
}

/**
 * RGB (0..255) to HSV (h in [0,360), s/v in [0,1]). Pure Kotlin mirror of
 * android.graphics.Color.RGBToHSV so scoring stays unit-testable.
 */
internal fun rgbToHsv(
    r: Int,
    g: Int,
    b: Int,
): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    val h =
        when {
            delta == 0f -> 0f
            max == rf -> 60f * (((gf - bf) / delta) % 6f)
            max == gf -> 60f * ((bf - rf) / delta + 2f)
            else -> 60f * ((rf - gf) / delta + 4f)
        }
    val s = if (max == 0f) 0f else delta / max
    return floatArrayOf((h + 360f) % 360f, s, max)
}
