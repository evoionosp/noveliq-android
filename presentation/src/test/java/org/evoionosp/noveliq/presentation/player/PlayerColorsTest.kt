package org.evoionosp.noveliq.presentation.player

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.evoionosp.noveliq.presentation.utils.rgbToHsv
import org.evoionosp.noveliq.presentation.utils.scoreVibrantSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerColorsTest {
    // rgbToHsv ----

    @Test
    fun `red converts to hue zero full saturation`() {
        val hsv = rgbToHsv(255, 0, 0)
        assertEquals(0f, hsv[0], 0.5f)
        assertEquals(1f, hsv[1], 0.001f)
        assertEquals(1f, hsv[2], 0.001f)
    }

    @Test
    fun `green and blue land on their hues`() {
        assertEquals(120f, rgbToHsv(0, 255, 0)[0], 0.5f)
        assertEquals(240f, rgbToHsv(0, 0, 255)[0], 0.5f)
    }

    @Test
    fun `gray has no saturation`() {
        val hsv = rgbToHsv(128, 128, 128)
        assertEquals(0f, hsv[1], 0.001f)
        assertEquals(128 / 255f, hsv[2], 0.001f)
    }

    // scoreVibrantSeed ----

    @Test
    fun `mostly gray art with a yellow element seeds yellow`() {
        val pixels = IntArray(48) { 0xFF808080.toInt() } + IntArray(16) { 0xFFFFD600.toInt() }
        assertEquals(0xFFFFD600.toInt(), scoreVibrantSeed(pixels))
    }

    @Test
    fun `all gray art falls back to the average`() {
        val pixels = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(0xFF7F7F7F.toInt(), scoreVibrantSeed(pixels))
    }

    @Test
    fun `scoring requires at least one pixel`() {
        assertThrows(IllegalArgumentException::class.java) { scoreVibrantSeed(intArrayOf()) }
    }

    // vibrantPlayerColors ----

    @Test
    fun `dark scheme pops a light accent near the seed hue`() {
        val roles =
            vibrantPlayerColors(
                seedArgb = 0xFFFFD600.toInt(),
                background = Color(0xFF1B1B1B),
                darkTheme = true,
            )
        assertTrue(roles.accent.luminance() > 0.4f)
        assertTrue(hueDiff(hueOf(roles.accent), 52f) < 35f)
        assertTrue(contrastRatio(roles.accent, roles.onAccent) >= 4.0)
        assertTrue(contrastRatio(roles.textPrimary, Color(0xFF1B1B1B)) >= 4.0)
    }

    @Test
    fun `light scheme pops a dark accent with contrast-correct pairs`() {
        val background = Color(0xFFFAFAFA)
        val roles =
            vibrantPlayerColors(
                seedArgb = 0xFFFFD600.toInt(),
                background = background,
                darkTheme = false,
            )
        assertTrue(roles.accent.luminance() < 0.5f)
        assertTrue(contrastRatio(roles.accent, roles.onAccent) >= 4.0)
        assertTrue(contrastRatio(roles.textPrimary, background) >= 4.0)
    }

    @Test
    fun `gray seed yields honest gray accents`() {
        val roles =
            vibrantPlayerColors(
                seedArgb = 0xFF808080.toInt(),
                background = Color(0xFF1B1B1B),
                darkTheme = true,
            )
        assertTrue(rgbToHsv(roles.accent.redInt(), roles.accent.greenInt(), roles.accent.blueInt())[1] < 0.12f)
        assertTrue(contrastRatio(roles.accent, roles.onAccent) >= 4.0)
    }

    @Test
    fun `subtle tones mix background toward the accent`() {
        val background = Color(0xFF1B1B1B)
        val roles =
            vibrantPlayerColors(
                seedArgb = 0xFFFFD600.toInt(),
                background = background,
                darkTheme = true,
            )
        assertEquals(
            background.red * 0.84f + roles.accent.red * 0.16f,
            roles.containerSubtle.red,
            0.002f,
        )
        assertEquals(
            background.red * 0.72f + roles.accent.red * 0.28f,
            roles.trackSubtle.red,
            0.002f,
        )
    }

    // playerColorsFallback ----

    @Test
    fun `fallback maps theme roles one to one`() {
        val scheme =
            lightColorScheme(
                primary = Color.Red,
                onPrimary = Color.Green,
                onSurface = Color.Blue,
                onSurfaceVariant = Color.Yellow,
                surfaceContainerHigh = Color.Cyan,
                surfaceVariant = Color.Magenta,
            )
        val roles = playerColorsFallback(scheme)
        assertEquals(Color.Red, roles.accent)
        assertEquals(Color.Green, roles.onAccent)
        assertEquals(Color.Blue, roles.textPrimary)
        assertEquals(Color.Yellow, roles.textSecondary)
        assertEquals(Color.Cyan, roles.containerSubtle)
        assertEquals(Color.Magenta, roles.trackSubtle)
    }

    @Test
    fun `card keeps tint when theme from cover is on`() {
        val surface = Color(0xFF1B1B1B)
        val dominant = Color(0xFFFFD600)
        val got = playerCardColor(surface, dominant, true, 0.38f)
        assertEquals(surface.red * 0.62f + dominant.red * 0.38f, got.red, 0.002f)
        assertEquals(surface.green * 0.62f + dominant.green * 0.38f, got.green, 0.002f)
        assertEquals(surface.blue * 0.62f + dominant.blue * 0.38f, got.blue, 0.002f)
    }

    @Test
    fun `card falls back to surface without dominant color`() {
        val surface = Color(0xFF1B1B1B)
        assertEquals(surface, playerCardColor(surface, null, true, 0.38f))
    }

    @Test
    fun `card ignores dominant color when theme from cover is off`() {
        val surface = Color(0xFF1B1B1B)
        assertEquals(surface, playerCardColor(surface, Color(0xFFFFD600), false, 0.38f))
    }

    private fun hueOf(color: Color): Float = rgbToHsv(color.redInt(), color.greenInt(), color.blueInt())[0]

    private fun hueDiff(
        a: Float,
        b: Float,
    ): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    private fun contrastRatio(
        a: Color,
        b: Color,
    ): Double {
        val l1 = a.luminance().toDouble() + 0.05
        val l2 = b.luminance().toDouble() + 0.05
        return max(l1, l2) / min(l1, l2)
    }

    private fun Color.redInt(): Int = (red * 255).toInt()

    private fun Color.greenInt(): Int = (green * 255).toInt()

    private fun Color.blueInt(): Int = (blue * 255).toInt()
}
