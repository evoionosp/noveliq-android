package org.evoionosp.noveliq.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.utils.CoverArt
import org.evoionosp.noveliq.presentation.utils.SheetDragState
import org.evoionosp.noveliq.presentation.utils.rememberArtPalette

private val MINI_SIDE_PADDING = 12.dp
internal val MINI_CORNER_RADIUS = 16.dp
internal val MINI_BOTTOM_GAP = 8.dp
private val MINI_FALLBACK_HEIGHT = 64.dp
private val FULL_PARALLAX_DISTANCE = 28.dp

/**
 * Staged handoff, deliberately gapped: the mini is fully out by 0.15 and the
 * full content starts fading in at 0.22, so the two never read as a double
 * player mid-morph — the bare card bridges the beat between them.
 */
private const val MINI_GONE_BELOW_FRACTION = 0.15f
private const val MINI_VISIBLE_BELOW_FRACTION = 0.18f
private const val FULL_FADES_ABOVE_FRACTION = 0.22f
private const val FULL_VISIBLE_ABOVE_FRACTION = 0.20f
private const val MINI_TAP_BELOW_FRACTION = 0.15f

/** Cover-tint strength on the sheet background; the rest stays theme surface. */
private const val PLAYER_TINT_BLEND = 0.38f

/**
 * Unified Now Playing sheet. One container morphs from the mini card into the
 * full screen, driven by [sheetDrag]'s expansion fraction:
 * - card rect (padded, rounded, flat surface) at 0 to full-bleed at 1,
 * - mini content fading out early, full content fading in with a slight rise.
 *
 * A single finger drag can therefore span both surfaces: dragging up from the
 * mini card grows the sheet out of the card itself instead of sliding a
 * separate panel over it. Overshoot past either anchor stays in the draw phase
 * so the rubber band never relayouts content.
 */
@Composable
fun NowPlayingOverlay(
    targetExpanded: Boolean,
    sheetDrag: SheetDragState,
    playingAudiobook: Audiobook?,
    themeFromCover: Boolean,
    bottomInsetDp: Dp,
    onMinimize: () -> Unit,
    onExpandMini: () -> Unit,
    onMiniHeightKnown: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Latest-callback holders: these lambdas are recreated by the parent on
    // every recomposition.
    val currentOnMinimize by rememberUpdatedState(onMinimize)
    val currentOnExpandMini by rememberUpdatedState(onExpandMini)
    val currentOnMiniHeightKnown by rememberUpdatedState(onMiniHeightKnown)
    // Natural mini height, measured once the mini card composes. Declared
    // before the early returns: conditional remembers would corrupt slots.
    var miniHeightPx by remember { mutableFloatStateOf(0f) }
    // Landing pulse: a stretch on expand, a squash-and-spring on collapse,
    // both anchored to the card's bottom edge where it lands. Applied on the
    // card itself — the outer box is full-size, so scaling it would pivot at
    // the screen bottom and shove the card down instead of squashing it.
    val landingScaleY = remember { Animatable(1f) }
    LaunchedEffect(targetExpanded) {
        if (targetExpanded) {
            landingScaleY.snapTo(1f)
            landingScaleY.animateTo(
                targetValue = 1f,
                animationSpec =
                    keyframes {
                        durationMillis = 250
                        1f at 0
                        1.05f at 125
                        1f at 250
                    },
            )
        } else {
            landingScaleY.snapTo(0.96f)
            landingScaleY.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        }
    }
    val offset = sheetDrag.offsetPx.floatValue
    val travel = sheetDrag.travelPx

    BackHandler(
        enabled = playingAudiobook != null && (targetExpanded || offset < travel),
    ) {
        currentOnMinimize()
    }

    if (playingAudiobook == null) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        LaunchedEffect(maxHeightPx) {
            sheetDrag.onTravelKnown(maxHeightPx)
        }
        // Travel unknown: report it but draw nothing yet, so the first frame
        // can never flash the sheet at the expanded position.
        if (!sheetDrag.travelKnown) return@BoxWithConstraints

        val fraction = sheetDrag.expansionFraction

        val miniHeightOrFallbackPx =
            if (miniHeightPx > 0f) miniHeightPx else with(density) { MINI_FALLBACK_HEIGHT.toPx() }
        val bottomTotalPx = with(density) { (bottomInsetDp + MINI_BOTTOM_GAP).toPx() }
        val collapsedTopPx = (maxHeightPx - bottomTotalPx - miniHeightOrFallbackPx).coerceAtLeast(0f)
        val eased = FastOutSlowInEasing.transform(fraction)
        // Layout position of the card; live overshoot is applied separately in
        // the draw phase below so it never triggers relayout.
        val cardTopPx = (1f - eased) * collapsedTopPx
        val cardBottomPx = maxHeightPx - bottomTotalPx * (1f - eased)
        val cardHeightPx = (cardBottomPx - cardTopPx).coerceAtLeast(0f)
        val overshootPx = offset - (1f - fraction) * travel

        // One cover-tinted background shared by mini and full, so there is no
        // color seam anywhere in the morph. Seeded from the playing book.
        // Animated: sampling resolves asynchronously and books change, and
        // neither should pop.
        val surfaceColor = MaterialTheme.colorScheme.surface
        val tintBook = playingAudiobook
        val artPalette =
            tintBook?.let {
                rememberArtPalette(
                    authorizedImageRequest(
                        CoverArt(url = it.coverUrl, title = it.title, author = it.author),
                    ),
                )
            }
        val dominantColor = artPalette?.dominant
        val cardColorTarget =
            playerCardColor(
                surface = surfaceColor,
                dominant = dominantColor,
                themeFromCover = themeFromCover,
                blend = PLAYER_TINT_BLEND,
            )
        val cardColor by animateColorAsState(targetValue = cardColorTarget, label = "PlayerCardTint")
        // Scoped player theme: every component inside the card follows the
        // cover, while the rest of the app keeps the user theme. The scheme
        // variant follows the app theme via the untinted surface — never the
        // tinted card, whose mid-tone luminance would sit on the threshold.
        // Sampling keeps running while the toggle is off so turning it back
        // on applies instantly from the cache.
        val playerColors =
            rememberPlayerColors(
                seed = if (themeFromCover) artPalette?.vibrant else null,
                background = cardColor,
                darkTheme = surfaceColor.luminance() < 0.5f,
            )
        val sidePadding = MINI_SIDE_PADDING * (1f - eased)
        val cornerRadius = MINI_CORNER_RADIUS * (1f - eased)
        val miniAlpha = (1f - fraction / MINI_GONE_BELOW_FRACTION).coerceIn(0f, 1f)
        val fullAlpha =
            ((fraction - FULL_FADES_ABOVE_FRACTION) / (1f - FULL_FADES_ABOVE_FRACTION))
                .coerceIn(0f, 1f)
        val parallaxPx = with(density) { FULL_PARALLAX_DISTANCE.toPx() } * (1f - eased)
        val showMini = playingAudiobook != null && fraction < MINI_VISIBLE_BELOW_FRACTION
        val showFull = fraction > FULL_VISIBLE_ABOVE_FRACTION

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, cardTopPx.roundToInt()) }
                    .graphicsLayer {
                        translationY = overshootPx
                    }.padding(horizontal = sidePadding),
        ) {
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = cardColor,
                // Explicit: a raw tinted mix has no scheme content color, so
                // implicit text/icon colors would resolve to black. The player
                // text role keeps them on-palette instead.
                contentColor = playerColors.textPrimary,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp * (1f - eased),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(with(density) { cardHeightPx.toDp() })
                        // Landing pulse lives on the card itself: scaling the
                        // outer full-size box instead would pivot at the screen
                        // bottom and shove the card down rather than squash it.
                        .graphicsLayer {
                            scaleY = landingScaleY.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // Explicit mask: the fixed-height full content
                            // below always overflows this card mid-morph.
                            .clip(RoundedCornerShape(cornerRadius)),
                ) {
                    if (showFull) {
                        // requiredHeight, not height: plain height would be
                        // coerced down to the shrinking card and reflow the
                        // artwork again. This pins the full layout at screen
                        // size, top-aligned, so the card reveals it instead of
                        // squishing it.
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .requiredHeight(with(density) { maxHeightPx.toDp() })
                                    .graphicsLayer {
                                        alpha = fullAlpha
                                        translationY = parallaxPx
                                    },
                        ) {
                            CompositionLocalProvider(LocalPlayerColors provides playerColors) {
                                NowPlayingScreen(
                                    onMinimize = onMinimize,
                                    sheetDrag = sheetDrag,
                                )
                            }
                        }
                    }
                    if (showMini && playingAudiobook != null) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        currentOnMiniHeightKnown(with(density) { size.height.toDp() })
                                    }.graphicsLayer { alpha = miniAlpha }
                                    .pointerInput(sheetDrag) {
                                        val tracker = VelocityTracker()
                                        detectVerticalDragGestures(
                                            onDragStart = { tracker.resetTracking() },
                                            onDragEnd = {
                                                sheetDrag.onDragEnd(tracker.calculateVelocity().y)
                                            },
                                            onDragCancel = { sheetDrag.onDragCancel() },
                                            onVerticalDrag = { change, dragDelta ->
                                                tracker.addPosition(change.uptimeMillis, change.position)
                                                sheetDrag.dragBy(dragDelta)
                                            },
                                        )
                                    }.clickable(
                                        enabled = fraction < MINI_TAP_BELOW_FRACTION,
                                        onClick = { currentOnExpandMini() },
                                    ),
                        ) {
                            CompositionLocalProvider(LocalPlayerColors provides playerColors) {
                                MiniPlayerContent(audiobook = playingAudiobook)
                            }
                        }
                    }
                }
            }
        }
    }
}
