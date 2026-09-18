package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.evoionosp.noveliq.presentation.common.LocalAccessToken
import org.evoionosp.noveliq.presentation.permissions.RequestNotificationPermissionEffect
import org.evoionosp.noveliq.presentation.player.LocalPlayerColors
import org.evoionosp.noveliq.presentation.player.MINI_BOTTOM_GAP
import org.evoionosp.noveliq.presentation.player.NowPlayingOverlay
import org.evoionosp.noveliq.presentation.player.NowPlayingViewModel
import org.evoionosp.noveliq.presentation.player.playerColorsFallback
import org.evoionosp.noveliq.presentation.settings.SettingsUiState
import org.evoionosp.noveliq.presentation.splash.SplashUiState
import org.evoionosp.noveliq.presentation.splash.StartupDestination
import org.evoionosp.noveliq.presentation.theme.ThemePreference
import org.evoionosp.noveliq.presentation.utils.SheetDragState

val LocalSnackbarHostState =
    compositionLocalOf<SnackbarHostState> {
        error("No SnackbarHostState provided")
    }

@Composable
fun <T> ObserveAsEvents(
    flow: SharedFlow<T>,
    onEvent: suspend CoroutineScope.(T) -> Unit,
) {
    LaunchedEffect(flow) {
        flow.collectLatest { event ->
            onEvent(event)
        }
    }
}

@Composable
fun NoveliqApp(
    splashState: SplashUiState,
    settingsState: SettingsUiState,
    onRetryCatalogBootstrap: () -> Unit,
    onLogout: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCoverThemeChange: (Boolean) -> Unit,
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val baseRoute =
        when (splashState.startupDestination) {
            StartupDestination.Auth -> AppRoute.Auth
            is StartupDestination.Home -> AppRoute.Home
            is StartupDestination.CatalogLoadError -> AppRoute.CatalogError
        }

    // Key on the route only. Keying on the full startupDestination would rebuild the whole
    // NavHost (resetting navigation state) whenever the session token rotates; the route is
    // what actually determines the graph, and token changes flow through via recomposition.
    key(baseRoute) {
        val navController = rememberNavController()
        val nowPlayingUiState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
        val playingAudiobook = nowPlayingUiState.playback.audiobook
        // Saveable, not plain remember: rotation recreates the Activity, and the
        // expanded player must survive that (playback itself continues regardless).
        var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
        // Single sheet state shared by the mini bar and the full sheet, so one
        // finger drag can span both. Settling collapsed reports back through
        // the target flag above.
        val sheetScope = rememberCoroutineScope()
        val sheetDrag =
            remember(sheetScope) {
                SheetDragState(
                    scope = sheetScope,
                    onCollapsed = { isNowPlayingExpanded = false },
                    onExpanded = { isNowPlayingExpanded = true },
                )
            }
        LaunchedEffect(isNowPlayingExpanded, sheetDrag.travelKnown) {
            if (!sheetDrag.travelKnown) return@LaunchedEffect
            if (isNowPlayingExpanded) {
                sheetDrag.expand()
            } else {
                sheetDrag.collapse()
            }
        }
        // Scaffold bottom inset (nav bar + system inset) anchors the collapsed
        // card; the measured mini height reserves catalog space for it while
        // something is playing.
        var bottomInsetDp by remember { mutableStateOf(0.dp) }
        var miniHeightDp by remember { mutableStateOf(0.dp) }
        val layoutDirection = LocalLayoutDirection.current
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val homeDestination = splashState.startupDestination as? StartupDestination.Home
        val accessToken = homeDestination?.session?.accessToken.orEmpty()

        if (homeDestination != null) {
            RequestNotificationPermissionEffect()
        }

        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalAccessToken provides accessToken,
            LocalPlayerColors provides playerColorsFallback(MaterialTheme.colorScheme),
        ) {
            val showSearchFab =
                currentRoute == AppRoute.Home.route || currentRoute == AppRoute.Library.route

            var isFabVisible by remember { mutableStateOf(true) }
            val nestedScrollConnection =
                remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            if (available.y < -1) {
                                isFabVisible = false
                            } else if (available.y > 1) {
                                isFabVisible = true
                            }
                            return Offset.Zero
                        }
                    }
                }

            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        RootNavigationBottomBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            sheetDrag = sheetDrag,
                        )
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = showSearchFab && isFabVisible,
                            enter = scaleIn(),
                            exit = scaleOut(),
                        ) {
                            FloatingActionButton(onClick = { /* TODO: Implement Search */ }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                        }
                    },
                ) { innerPadding ->
                    bottomInsetDp = innerPadding.calculateBottomPadding()
                    // Trailing scroll buffer (mini height + its bottom gap) for
                    // destinations with their own scaffold; 0 when idle so no
                    // phantom space is ever reserved.
                    val miniTrailPadding =
                        if (playingAudiobook != null) {
                            miniHeightDp + MINI_BOTTOM_GAP
                        } else {
                            0.dp
                        }
                    NoveliqNavHost(
                        navController = navController,
                        startDestination = baseRoute.route,
                        splashState = splashState,
                        settingsState = settingsState,
                        contentPadding =
                            PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                top = innerPadding.calculateTopPadding(),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                bottom =
                                    innerPadding.calculateBottomPadding() +
                                        if (playingAudiobook != null) miniHeightDp else 0.dp,
                            ),
                        miniTrailPadding = miniTrailPadding,
                        onRetryCatalogBootstrap = onRetryCatalogBootstrap,
                        onLogout = onLogout,
                        onThemePreferenceChange = onThemePreferenceChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onCoverThemeChange = onCoverThemeChange,
                        onOpenAudiobook = { audiobook ->
                            navController.navigate(
                                bookDetailsRoute(audiobook.libraryId, audiobook.id),
                            )
                        },
                    )
                }

                NowPlayingOverlay(
                    targetExpanded = isNowPlayingExpanded,
                    sheetDrag = sheetDrag,
                    playingAudiobook = playingAudiobook,
                    themeFromCover = settingsState.useCoverTheme,
                    bottomInsetDp = bottomInsetDp,
                    onMinimize = { isNowPlayingExpanded = false },
                    onExpandMini = { isNowPlayingExpanded = true },
                    onMiniHeightKnown = { miniHeightDp = it },
                )
            }
        }
    }
}
