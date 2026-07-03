package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.evoionosp.noveliq.presentation.MainActivity
import org.evoionosp.noveliq.presentation.player.NowPlayingOverlay
import org.evoionosp.noveliq.presentation.player.NowPlayingViewModel
import org.evoionosp.noveliq.presentation.settings.SettingsUiState
import org.evoionosp.noveliq.presentation.splash.SplashUiState
import org.evoionosp.noveliq.presentation.splash.StartupDestination

@Composable
fun NoveliqApp(
    splashState: SplashUiState,
    settingsState: SettingsUiState,
    onRetryCatalogBootstrap: () -> Unit,
    onLogout: () -> Unit,
    onThemePreferenceChange: (org.evoionosp.noveliq.presentation.ui.theme.ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
) {
    val baseRoute = when (splashState.startupDestination) {
        StartupDestination.Auth -> AppRoute.Auth
        is StartupDestination.Home -> AppRoute.Home
        is StartupDestination.CatalogLoadError -> AppRoute.CatalogError
    }


    key(baseRoute, splashState.startupDestination) {
        val navController = rememberNavController()
        val nowPlayingUiState by nowPlayingViewModel.uiState.collectAsStateWithLifecycle()
        val playingAudiobook = nowPlayingUiState.playback.audiobook
        val viewedAudiobook = nowPlayingUiState.viewedAudiobook
        var isNowPlayingExpanded by remember { mutableStateOf(false) }
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val homeDestination = splashState.startupDestination as? StartupDestination.Home
        val accessToken = homeDestination?.session?.accessToken.orEmpty()

        Box {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    RootNavigationBottomBar(
                        navController = navController,
                        currentRoute = currentRoute,
                        nowPlayingAudiobook = playingAudiobook,
                        nowPlayingAccessToken = accessToken,
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
                    bottomBarPadding = innerPadding.calculateBottomPadding(),
                    onRetryCatalogBootstrap = onRetryCatalogBootstrap,
                    onLogout = onLogout,
                    onThemePreferenceChange = onThemePreferenceChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onOpenAudiobook = { audiobook ->
                        nowPlayingViewModel.openAudiobook(audiobook)
                        isNowPlayingExpanded = true
                    }
                )
            }

            NowPlayingOverlay(
                visible = isNowPlayingExpanded,
                audiobook = viewedAudiobook,
                accessToken = accessToken,
                onMinimize = { isNowPlayingExpanded = false }
            )
        }
    }
}
