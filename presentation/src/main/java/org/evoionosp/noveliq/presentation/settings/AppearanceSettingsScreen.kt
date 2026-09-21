package org.evoionosp.noveliq.presentation.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.theme.ThemePreference

@Composable
fun AppearanceSettingsScreen(
    settingsState: SettingsUiState,
    onBackClick: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCoverThemeChange: (Boolean) -> Unit,
    miniTrailPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = SETTINGS_MIN_BAR_HEIGHT + statusBarHeight
    val maxTopBarHeight = SETTINGS_MAX_HEADER_HEIGHT

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
        val headerHeightDp = with(density) { topBarHeight.value.toDp() }
        val navigationBarHeight =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = headerHeightDp + 8.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = navigationBarHeight + 24.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.preferences_appearance_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                PreferenceGroup(
                    title = stringResource(R.string.preferences_app_theme),
                ) {
                    ThemePreferenceRow(
                        label = stringResource(R.string.theme_system_default),
                        description = stringResource(R.string.theme_system_default_desc),
                        selected = settingsState.themePreference == ThemePreference.SYSTEM,
                        onClick = { onThemePreferenceChange(ThemePreference.SYSTEM) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    ThemePreferenceRow(
                        label = stringResource(R.string.theme_dark),
                        description = stringResource(R.string.theme_dark_desc),
                        selected = settingsState.themePreference == ThemePreference.DARK,
                        onClick = { onThemePreferenceChange(ThemePreference.DARK) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    ThemePreferenceRow(
                        label = stringResource(R.string.theme_light),
                        description = stringResource(R.string.theme_light_desc),
                        selected = settingsState.themePreference == ThemePreference.LIGHT,
                        onClick = { onThemePreferenceChange(ThemePreference.LIGHT) },
                    )
                }
            }

            item {
                PreferenceGroup(
                    title = stringResource(R.string.preferences_color),
                ) {
                    PreferenceSwitchRow(
                        label = stringResource(R.string.use_dynamic_color),
                        description = stringResource(R.string.use_dynamic_color_desc),
                        checked = settingsState.useDynamicColor,
                        onCheckedChange = onDynamicColorChange,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    PreferenceSwitchRow(
                        label = stringResource(R.string.now_playing_cover_theme),
                        description = stringResource(R.string.now_playing_cover_theme_desc),
                        checked = settingsState.useCoverTheme,
                        onCheckedChange = onCoverThemeChange,
                    )
                }
            }
        }
        CollapsingSettingsHeader(
            title = stringResource(R.string.preferences_appearance),
            collapseFraction = collapseFraction,
            headerHeight = headerHeightDp,
            onBackClick = onBackClick,
        )
    }
}

@Composable
private fun PreferenceGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // PixelPlayerOSS subsection style: small primary label, bumped to
        // 16sp Bold so Montserrat matches their chunkier 14sp rendering.
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp),
        )

        Surface(
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun ThemePreferenceRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                ).padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun PreferenceSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
