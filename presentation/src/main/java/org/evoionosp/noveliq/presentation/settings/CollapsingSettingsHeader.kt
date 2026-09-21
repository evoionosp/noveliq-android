package org.evoionosp.noveliq.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import org.evoionosp.noveliq.presentation.R

internal val SETTINGS_MIN_BAR_HEIGHT = 64.dp
internal val SETTINGS_MAX_HEADER_HEIGHT = 180.dp

/**
 * Collapsing settings header, PixelPlayerOSS Settings-shaped: the title
 * rides from the header bottom up into the collapsed bar as the list
 * scrolls, over a background fading from transparent to
 * surfaceContainerHigh. The back button stays the tonal Preferences one.
 *
 * Full-bleed by design (draws under the status bar), so the host must NOT
 * apply the top window inset here — the NavHost already passes no content
 * padding to the settings destinations.
 */
@Composable
internal fun CollapsingSettingsHeader(
    title: String,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackClick: () -> Unit,
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val solidAlpha = (fraction * 2f).coerceIn(0f, 1f)
    val titleScale = lerp(1.2f, 0.8f, fraction)
    val titlePaddingStart = lerp(20.dp, 68.dp, fraction)
    val titleVerticalBias = lerp(1f, -1f, fraction)
    val animatedTitleAlignment =
        BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, fraction)
    val titleStyle = MaterialTheme.typography.headlineMedium

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = solidAlpha),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            FilledTonalIconButton(
                onClick = onBackClick,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.preferences_back),
                )
            }
            Box(
                modifier =
                    Modifier
                        .align(animatedTitleAlignment)
                        .height(titleContainerHeight)
                        .fillMaxWidth()
                        .padding(start = titlePaddingStart, end = 24.dp),
            ) {
                Text(
                    text = title,
                    style =
                        titleStyle.copy(
                            fontSize = titleStyle.fontSize * titleScale,
                            lineHeight = titleStyle.fontSize * titleScale * 1.1f,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
    }
}
