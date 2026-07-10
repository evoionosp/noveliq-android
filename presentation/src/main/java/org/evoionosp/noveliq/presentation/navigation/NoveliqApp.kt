package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.evoionosp.noveliq.presentation.common.LocalAccessToken
import org.evoionosp.noveliq.presentation.common.model.toDomain
import org.evoionosp.noveliq.presentation.permissions.RequestNotificationPermissionEffect
import org.evoionosp.noveliq.presentation.player.NowPlayingOverlay
import org.evoionosp.noveliq.presentation.player.NowPlayingViewModel
import org.evoionosp.noveliq.presentation.settings.SettingsUiState
import org.evoionosp.noveliq.presentation.splash.SplashUiState
import org.evoionosp.noveliq.presentation.splash.StartupDestination
import org.evoionosp.noveliq.presentation.theme.ThemePreference

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun <T> ObserveAsEvents(flow: SharedFlow<T>, onEvent: suspend CoroutineScope.(T) -> Unit) {
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
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val baseRoute = when (splashState.startupDestination) {
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
        val viewedAudiobook = nowPlayingUiState.viewedAudiobook
        var isNowPlayingExpanded by remember { mutableStateOf(false) }
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val homeDestination = splashState.startupDestination as? StartupDestination.Home
        val accessToken = homeDestination?.session?.accessToken.orEmpty()

        if (homeDestination != null) {
            RequestNotificationPermissionEffect()
        }

        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalAccessToken provides accessToken
        ) {
            Box {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        RootNavigationBottomBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            nowPlayingAudiobook = playingAudiobook,
                            isNowPlayingExpanded = isNowPlayingExpanded,
                            onExpandNowPlaying = {
                                nowPlayingViewModel.viewCurrentlyPlaying()
                                isNowPlayingExpanded = true
                            }
                        )
                    }
                ) { innerPadding ->
                    NoveliqNavHost(
                        navController = navController,
                        startDestination = baseRoute.route,
                        splashState = splashState,
                        settingsState = settingsState,
                        contentPadding = innerPadding,
                        onRetryCatalogBootstrap = onRetryCatalogBootstrap,
                        onLogout = onLogout,
                        onThemePreferenceChange = onThemePreferenceChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onOpenAudiobook = { audiobook ->
                            nowPlayingViewModel.openAudiobook(audiobook.toDomain())
                            isNowPlayingExpanded = true
                        }
                    )
                }

                NowPlayingOverlay(
                    visible = isNowPlayingExpanded,
                    audiobook = viewedAudiobook,
                    onMinimize = { isNowPlayingExpanded = false }
                )
            }
        }
    }
}
