package org.evoionosp.noveliq.presentation.bookdetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.player.BookCoverArtwork
import org.evoionosp.noveliq.presentation.player.BookProgressBar
import org.evoionosp.noveliq.presentation.player.ChapterRow
import org.evoionosp.noveliq.presentation.player.PlayPauseGlyph
import org.evoionosp.noveliq.presentation.player.animatePlayPauseCorner
import org.evoionosp.noveliq.presentation.player.inProgressChapterIndex

private const val DETAILS_HEADER_HEIGHT_FRACTION = 0.4f
private val DETAILS_HEADER_MIN_BAR_HEIGHT = 64.dp

/**
 * Portrait book details, PixelPlayer album-screen shaped: a full-bleed cover
 * header that collapses into a top bar as the chapter list scrolls beneath
 * it, instead of the old single scrolling column. Stateless by design: all
 * data and actions come from the caller.
 *
 * The header draws under the status bar (with its own legibility scrim), so
 * the host must NOT apply the top window inset here — only the bottom one.
 *
 * @param onTogglePlayPause header play action; pauses when this book is playing.
 * @param onBackClick header back arrow; the route shows no app bar in portrait.
 * @param listState hoisted so the host can share it; drives the collapse maths.
 * @param miniTrailPadding mini height + gap appended at the end of the list so
 * fully scrolled content clears the overlay bar; 0 when idle.
 */
@Composable
internal fun BookDetailsContent(
    modifier: Modifier = Modifier,
    audiobook: Audiobook,
    bookProgress: Float,
    chapters: List<AudiobookChapter>,
    inProgressSeconds: Double,
    isPlaying: Boolean,
    onPlayChapter: (AudiobookChapter) -> Unit,
    onTogglePlayPause: () -> Unit,
    onBackClick: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    miniTrailPadding: Dp = 0.dp,
) {
    val inProgressIndex = inProgressChapterIndex(chapters, inProgressSeconds)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = DETAILS_HEADER_MIN_BAR_HEIGHT + statusBarHeight
    val maxTopBarHeight = (configuration.screenHeightDp * DETAILS_HEADER_HEIGHT_FRACTION).dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f -
                ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(
                    0f,
                    1f,
                )
        }
    }

    val nestedScrollConnection =
        remember(listState, minTopBarHeightPx, maxTopBarHeightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val delta = available.y
                    val isScrollingDown = delta < 0

                    if (!isScrollingDown &&
                        (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
                    ) {
                        return Offset.Zero
                    }

                    val previousHeight = topBarHeight.value
                    val newHeight =
                        (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                    val consumed = newHeight - previousHeight

                    if (consumed.roundToInt() != 0) {
                        coroutineScope.launch {
                            topBarHeight.snapTo(newHeight)
                        }
                    }

                    val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                    return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity = super.onPostFling(consumed, available)
            }
        }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val shouldExpand =
                topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand =
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

            val targetValue =
                if (shouldExpand && canExpand) {
                    maxTopBarHeightPx
                } else {
                    minTopBarHeightPx
                }

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(
                        targetValue,
                        spring(stiffness = Spring.StiffnessMedium),
                    )
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset {
                        val extraHeight =
                            (topBarHeight.value - minTopBarHeightPx).roundToInt()
                        IntOffset(0, extraHeight)
                    },
            contentPadding =
                PaddingValues(
                    top = minTopBarHeight + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BookProgressBar(
                    progress = bookProgress,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50)),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (chapters.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.now_playing_chapters_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(chapters.size) { index ->
                    val chapter = chapters[index]
                    ChapterRow(
                        chapter = chapter,
                        isCurrent = index == inProgressIndex,
                        isPlaying = isPlaying && index == inProgressIndex,
                        onPlay = { onPlayChapter(chapter) },
                    )
                }
            }
        }

        CollapsingBookHeader(
            audiobook = audiobook,
            chaptersCount = chapters.size,
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            isPlaying = isPlaying,
            onBackClick = onBackClick,
            onPlayClick = onTogglePlayPause,
        )
    }
}

/**
 * Full-bleed cover header over a fading scrim: the art melts into the plain
 * surface at its bottom edge, the title block rides from the header bottom up
 * into the collapsed bar, and the play action shrinks away as it collapses.
 */
@Composable
private fun CollapsingBookHeader(
    audiobook: Audiobook,
    chaptersCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f
    val statusBarColor =
        if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)

    val fabScale = 1f - collapseFraction
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)
    val showExpandedArtwork = headerContentAlpha > 0.01f
    val headerOverlayBrush =
        remember(surfaceColor, headerContentAlpha) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        surfaceColor.copy(alpha = 0.30f * headerContentAlpha),
                        surfaceColor.copy(alpha = 0.90f * headerContentAlpha),
                        surfaceColor.copy(alpha = headerContentAlpha),
                    ),
            )
        }
    val statusBarBrush =
        remember(statusBarColor) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        statusBarColor,
                        Color.Transparent,
                    ),
            )
        }

    val titleScale = lerp(1f, 0.75f, collapseFraction)
    val titlePaddingStart = lerp(24.dp, 58.dp, collapseFraction)
    val titleMaxLines = if (collapseFraction < 0.5f) 2 else 1
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val animatedTitleAlignment =
        BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, collapseFraction)
    val yOffsetCorrection = lerp((titleContainerHeight / 2) - 64.dp, 0.dp, collapseFraction)
    val playCorner by animatePlayPauseCorner(isPlaying, 72.dp)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clipToBounds(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(surfaceColor.copy(alpha = backgroundAlpha)),
        ) {
            if (showExpandedArtwork) {
                BookCoverArtwork(
                    audiobook = audiobook,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(headerOverlayBrush),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(statusBarBrush)
                        .align(Alignment.TopCenter),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
            ) {
                FilledIconButton(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 4.dp),
                    onClick = onBackClick,
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
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
                            .offset(y = yOffsetCorrection),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = titlePaddingStart, end = 120.dp)
                                .graphicsLayer {
                                    scaleX = titleScale
                                    scaleY = titleScale
                                },
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = audiobook.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = titleMaxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                "${audiobook.author} • " +
                                    pluralStringResource(
                                        R.plurals.book_details_chapters_count,
                                        chaptersCount,
                                        chaptersCount,
                                    ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                FloatingActionButton(
                    onClick = onPlayClick,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                                alpha = fabScale
                            },
                    shape = RoundedCornerShape(playCorner),
                ) {
                    PlayPauseGlyph(
                        isPlaying = isPlaying,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconSize = 32.dp,
                        playContentDescription = stringResource(R.string.now_playing_play),
                        pauseContentDescription = stringResource(R.string.now_playing_pause),
                    )
                }
            }
        }
    }
}

/**
 * Text action shared by the landscape details page: same recipe as the login
 * button — full width, 14dp vertical padding, default fully-rounded
 * Material3 shape. (Portrait uses the collapsing header's play action.)
 */
@Composable
internal fun ContinueListeningButton(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onPlay,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text(text = stringResource(R.string.continue_listening))
    }
}
