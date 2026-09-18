@file:SuppressLint("RestrictedApi")

package org.evoionosp.noveliq.presentation.player

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeVibrant
import org.evoionosp.noveliq.presentation.utils.mixedWith
import org.evoionosp.noveliq.presentation.utils.rgbToHsv

/**
 * Art-derived roles for the mini + full player, PixelPlayer-shaped: a vibrant
 * dynamic scheme seeded from the cover, applied as a scoped theme over the
 * sheet while the rest of the app keeps the user theme. The background itself
 * stays the flatter card tint; these roles dress the components on top of it.
 */
internal data class PlayerColors(
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val containerSubtle: Color,
    val trackSubtle: Color,
)

/** Scoped player theme. Provided with theme roles at app level, overridden with art roles in the sheet. */
internal val LocalPlayerColors =
    compositionLocalOf<PlayerColors> { error("LocalPlayerColors not provided") }

/** Theme-based roles: the pre-load look, and the look outside the sheet. */
internal fun playerColorsFallback(scheme: ColorScheme): PlayerColors =
    PlayerColors(
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        containerSubtle = scheme.surfaceContainerHigh,
        trackSubtle = scheme.surfaceVariant,
    )

/** Seed saturation below which covers read as effectively gray (no hue worth popping). */
private const val MIN_VIBRANT_SEED_SATURATION = 0.18f

/**
 * Vibrant scheme roles from the cover seed — monochrome when the seed is
 * effectively gray, so neutral covers get honest gray accents instead of a
 * saturated arbitrary hue. Primary/text pairs come straight from the dynamic
 * scheme (contrast-correct by construction); the subtle container/track
 * tones stay glued to the card [background] with a hint of the accent so
 * pills and tracks read as one surface.
 */
internal fun vibrantPlayerColors(
    seedArgb: Int,
    background: Color,
    darkTheme: Boolean,
): PlayerColors {
    val seedSat =
        rgbToHsv(
            (seedArgb shr 16) and 0xFF,
            (seedArgb shr 8) and 0xFF,
            seedArgb and 0xFF,
        )[1]
    val source = Hct.fromInt(seedArgb)
    val scheme: DynamicScheme =
        if (seedSat >= MIN_VIBRANT_SEED_SATURATION) {
            SchemeVibrant(source, darkTheme, 0.0)
        } else {
            SchemeMonochrome(source, darkTheme, 0.0)
        }
    val roles = MaterialDynamicColors()
    val accent = Color(roles.primary().getArgb(scheme))
    return PlayerColors(
        accent = accent,
        onAccent = Color(roles.onPrimary().getArgb(scheme)),
        textPrimary = Color(roles.onSurface().getArgb(scheme)),
        textSecondary = Color(roles.onSurfaceVariant().getArgb(scheme)),
        containerSubtle = background.mixedWith(accent, 0.16f),
        trackSubtle = background.mixedWith(accent, 0.28f),
    )
}

/**
 * Card background target: the cover tint while the cover theme is on and a
 * dominant color resolved, otherwise the flat theme surface.
 */
internal fun playerCardColor(
    surface: Color,
    dominant: Color?,
    themeFromCover: Boolean,
    blend: Float,
): Color =
    if (themeFromCover && dominant != null) {
        surface.mixedWith(dominant, blend)
    } else {
        surface
    }

/**
 * Animated player roles: theme fallback until the cover seed resolves, then
 * a crossfade into the vibrant scheme (and between books). One animated
 * value per role keeps every component gliding coherently.
 */
@Composable
internal fun rememberPlayerColors(
    seed: Color?,
    background: Color,
    darkTheme: Boolean,
): PlayerColors {
    val target =
        if (seed != null) {
            vibrantPlayerColors(seed.toArgb(), background, darkTheme)
        } else {
            playerColorsFallback(MaterialTheme.colorScheme)
        }
    val accent by animateColorAsState(target.accent, label = "PlayerAccent")
    val onAccent by animateColorAsState(target.onAccent, label = "PlayerOnAccent")
    val textPrimary by animateColorAsState(target.textPrimary, label = "PlayerTextPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, label = "PlayerTextSecondary")
    val containerSubtle by animateColorAsState(target.containerSubtle, label = "PlayerContainer")
    val trackSubtle by animateColorAsState(target.trackSubtle, label = "PlayerTrack")
    return PlayerColors(
        accent = accent,
        onAccent = onAccent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        containerSubtle = containerSubtle,
        trackSubtle = trackSubtle,
    )
}
