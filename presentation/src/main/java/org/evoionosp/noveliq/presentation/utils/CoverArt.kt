package org.evoionosp.noveliq.presentation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation
import coil.compose.AsyncImage
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.ImageResult
import coil.request.SuccessResult
import org.evoionosp.noveliq.presentation.common.LocalAccessToken
import org.evoionosp.noveliq.presentation.player.authorizedImageRequest

/**
 * Cover-art request model. Carries the book identity alongside the URL so the
 * pipeline can synthesize a generated hardcover whenever the URL is missing
 * or the network load fails — every consumer then just loads art, with no
 * per-screen fallback branches.
 */
internal data class CoverArt(
    val url: String,
    val title: String,
    val author: String,
)

/**
 * Traditional hardcover cloth colors. All dark, so they sit calmly next to
 * real cover art in lists.
 */
private val COVER_PALETTE =
    listOf(
        Color(0xFF722F37), // burgundy
        Color(0xFF1C1C1E), // black cloth
        Color(0xFF2A2763), // indigo
        Color(0xFF1F4D2E), // forest
        Color(0xFF1B2A4A), // navy
        Color(0xFF3E2C22), // espresso
        Color(0xFF3D2B56), // deep plum
        Color(0xFF14424C), // dark teal
        Color(0xFF5C1A1B), // maroon
        Color(0xFF4A3B1F), // dark bronze
        Color(0xFF333A1F), // deep olive
        Color(0xFF2B3440), // dark slate
        Color(0xFF0F2A43), // prussian blue
        Color(0xFF2E1F16), // dark walnut
        Color(0xFF14181F), // ink
        Color(0xFF5E1224), // deep garnet
        Color(0xFF472B45), // aubergine
        Color(0xFF5A2E18), // deep copper
    )

private val COVER_TEXT = Color(0xFFF5ECD7)
private val GILT = Color(0xFFC9A227)

private const val GENERATED_WIDTH = 600
private const val GENERATED_HEIGHT = 600

/**
 * Deterministic cover color for a book, derived from its title and author so
 * the same book gets the same cloth on every device. [String.hashCode] is
 * spec-guaranteed stable, and the unit separator keeps "ab"+"c" distinct
 * from "a"+"bc".
 */
internal fun bookCoverColor(
    title: String,
    author: String,
): Color {
    val seed = "$title\u001F$author"
    return COVER_PALETTE[(seed.hashCode() and Int.MAX_VALUE) % COVER_PALETTE.size]
}

/**
 * Bakes the generated hardcover: cloth background, spine strip, gilt rule,
 * serif title top-center and author bottom-center. Rendered once at fixed
 * resolution, so it is pixel-identical at every display size. Total: never
 * throws, worst case a black pixel.
 */
internal fun generateCoverBitmap(
    title: String,
    author: String,
    width: Int = GENERATED_WIDTH,
    height: Int = GENERATED_HEIGHT,
): Bitmap {
    try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bookCoverColor(title, author).toArgb())

        val spineWidth = width * 0.09f
        val spinePaint =
            Paint().apply {
                color = AndroidColor.BLACK
                alpha = 77
            }
        canvas.drawRect(0f, 0f, spineWidth, height.toFloat(), spinePaint)
        val giltPaint =
            Paint().apply {
                color = GILT.toArgb()
                alpha = 128
            }
        canvas.drawRect(spineWidth + 8f, 0f, spineWidth + 11f, height.toFloat(), giltPaint)

        val textLeft = spineWidth + width * 0.05f
        val textWidth = (width - textLeft - width * 0.05f).toInt().coerceAtLeast(1)
        if (title.isNotEmpty()) {
            val titlePaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textSize = width * 0.105f
                    color = COVER_TEXT.toArgb()
                    letterSpacing = 0.04f
                }
            val titleLayout =
                StaticLayout.Builder
                    .obtain(title, 0, title.length, titlePaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(6)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            canvas.save()
            canvas.translate(textLeft, height * 0.09f)
            titleLayout.draw(canvas)
            canvas.restore()
        }
        if (author.isNotEmpty()) {
            val authorPaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    textSize = width * 0.068f
                    color = COVER_TEXT.toArgb()
                    alpha = 217
                    letterSpacing = 0.12f
                }
            val authorLayout =
                StaticLayout.Builder
                    .obtain(author, 0, author.length, authorPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
            canvas.withTranslation(textLeft, height * 0.92f - authorLayout.height) {
                authorLayout.draw(this)
            }
        }
        return bitmap
    } catch (_: Exception) {
        return createBitmap(1, 1).apply {
            eraseColor(AndroidColor.BLACK)
        }
    }
}

/**
 * Serves generated hardcovers from inside the Coil pipeline: blank URLs
 * synthesize immediately, and any network/decoding failure falls back to a
 * synthesized cover. Downstream loaders just work — no error branches.
 */
internal class CoverArtInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val model = chain.request.data
        if (model !is CoverArt) {
            return chain.proceed(chain.request)
        }
        if (model.url.isBlank()) {
            return generatedResult(chain, model)
        }
        // No fetcher understands CoverArt: translate back to the plain URL so
        // the default network stack serves it. Anything failing from here —
        // no route, HTTP error, undecodable bytes — gets a generated cover.
        val networkRequest =
            chain.request
                .newBuilder()
                .data(model.url)
                .build()
        val result =
            try {
                chain.proceed(networkRequest)
            } catch (_: Exception) {
                return generatedResult(chain, model)
            }
        if (result is ErrorResult) {
            return generatedResult(chain, model)
        }
        return result
    }

    private fun generatedResult(
        chain: Interceptor.Chain,
        model: CoverArt,
    ): ImageResult {
        val bitmap = generateCoverBitmap(model.title, model.author)
        val drawable = bitmap.toDrawable(chain.request.context.resources)
        return SuccessResult(drawable, chain.request, DataSource.MEMORY)
    }
}

/** Public entry for the app singleton; keeps the interceptor itself internal. */
fun newCoverArtInterceptor(): Interceptor = CoverArtInterceptor()

/**
 * Cover art with pipeline-generated hardcover fallback: real artwork when
 * [coverUrl] loads, a baked generated cover otherwise. The request is
 * remembered — a fresh instance every recomposition would restart the Coil
 * load and leave art stuck in loading on ticking screens.
 */
@Composable
internal fun BookCoverArtwork(
    coverUrl: String,
    title: String,
    author: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val accessToken = LocalAccessToken.current
    val art = CoverArt(url = coverUrl, title = title, author = author)
    val request =
        remember(art, accessToken) {
            authorizedImageRequest(context, accessToken, art)
        }
    AsyncImage(
        model = request,
        contentDescription = "$title by $author",
        modifier = modifier,
        contentScale = contentScale,
    )
}
