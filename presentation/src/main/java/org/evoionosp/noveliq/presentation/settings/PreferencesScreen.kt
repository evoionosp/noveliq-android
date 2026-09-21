package org.evoionosp.noveliq.presentation.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.presentation.R

@Composable
fun PreferencesScreen(
    onBackClick: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenServerConnection: () -> Unit,
    onLoggedOut: () -> Unit,
    showLogout: Boolean,
    miniTrailPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: PreferencesViewModel = hiltViewModel(),
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isLoggingOut by viewModel.isLoggingOut.collectAsStateWithLifecycle()
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
                    bottom = navigationBarHeight + 16.dp + miniTrailPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PreferenceListRow(
                            title = stringResource(R.string.preferences_appearance),
                            subtitle = stringResource(R.string.preferences_appearance_summary),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = onOpenAppearance,
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                        PreferenceListRow(
                            title = stringResource(R.string.preferences_server_connection),
                            subtitle = stringResource(R.string.preferences_server_connection_summary),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = onOpenServerConnection,
                        )
                    }
                }
            }
            if (showLogout) {
                item {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            PreferenceListRow(
                                title = stringResource(R.string.preferences_logout),
                                subtitle = stringResource(R.string.preferences_logout_summary),
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                },
                                titleColor = MaterialTheme.colorScheme.onErrorContainer,
                                subtitleColor =
                                    MaterialTheme.colorScheme.onErrorContainer.copy(
                                        alpha = 0.8f,
                                    ),
                                trailing = {},
                                onClick = { showLogoutDialog = true },
                            )
                        }
                    }
                }
            }
        }
        CollapsingSettingsHeader(
            title = stringResource(R.string.preferences_title),
            collapseFraction = collapseFraction,
            headerHeight = headerHeightDp,
            onBackClick = onBackClick,
        )
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.logout_confirm_title)) },
            text = { Text(text = stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout {
                            showLogoutDialog = false
                            onLoggedOut()
                        }
                    },
                    enabled = !isLoggingOut,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(text = stringResource(R.string.preferences_logout))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !isLoggingOut,
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PreferenceListRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
            }
            trailing()
        }
    }
}
