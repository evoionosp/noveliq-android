package org.evoionosp.noveliq.presentation

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.auth.AuthScreen
import org.evoionosp.noveliq.presentation.detail.AudiobookDetailScreen
import org.evoionosp.noveliq.presentation.home.HomeScreen
import org.evoionosp.noveliq.presentation.settings.AppearanceSettingsScreen
import org.evoionosp.noveliq.presentation.settings.PreferencesScreen
import org.evoionosp.noveliq.presentation.settings.SettingsViewModel
import org.evoionosp.noveliq.presentation.splash.CatalogBootstrapErrorScreen
import org.evoionosp.noveliq.presentation.splash.SplashScreen
import org.evoionosp.noveliq.presentation.splash.StartupDestination
import org.evoionosp.noveliq.presentation.splash.SplashViewModel
import org.evoionosp.noveliq.presentation.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    private enum class RootRoute(val route: String) {
        Auth("auth"),
        Home("home"),
        AudiobookDetail("audiobook/{libraryId}/{audiobookId}"),
        CatalogError("catalog_error"),
        Preferences("preferences"),
        Appearance("appearance")
    }

    private fun audiobookDetailRoute(libraryId: String, audiobookId: String): String {
        return "audiobook/${Uri.encode(libraryId)}/${Uri.encode(audiobookId)}"
    }

    private fun NavBackStackEntry.requireArg(name: String): String? {
        return arguments?.getString(name)?.takeIf { it.isNotBlank() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.uiState.value.isLoading
        }
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

            AppTheme(
                themePreference = settingsState.themePreference,
                dynamicColor = settingsState.useDynamicColor
            ) {
                val baseRoute = when (splashState.startupDestination) {
                    StartupDestination.Auth -> RootRoute.Auth
                    is StartupDestination.Home -> RootRoute.Home
                    is StartupDestination.CatalogLoadError -> RootRoute.CatalogError
                }

                if (splashState.isLoading) {
                    SplashScreen(modifier = Modifier)
                } else {
                    key(baseRoute, splashState.startupDestination) {
                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = baseRoute.route
                        ) {
                            composable(RootRoute.Auth.route) {
                                AuthScreen(
                                    modifier = Modifier,
                                    onOpenSettings = { navController.navigate(RootRoute.Preferences.route) }
                                )
                            }
                            composable(RootRoute.Home.route) {
                                val startupDestination = splashState.startupDestination as? StartupDestination.Home
                                    ?: return@composable
                                HomeScreen(
                                    username = startupDestination.session.username,
                                    accessToken = startupDestination.session.accessToken,
                                    onOpenSettings = { navController.navigate(RootRoute.Preferences.route) },
                                    onOpenAudiobook = { audiobook ->
                                        navController.navigate(
                                            audiobookDetailRoute(
                                                libraryId = audiobook.libraryId,
                                                audiobookId = audiobook.id
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                )
                            }
                            composable(RootRoute.AudiobookDetail.route) { backStackEntry ->
                                val startupDestination = splashState.startupDestination as? StartupDestination.Home
                                    ?: return@composable
                                if (backStackEntry.requireArg("libraryId") == null) {
                                    navController.popBackStack()
                                    return@composable
                                }
                                if (backStackEntry.requireArg("audiobookId") == null) {
                                    navController.popBackStack()
                                    return@composable
                                }
                                AudiobookDetailScreen(
                                    accessToken = startupDestination.session.accessToken,
                                    onBackClick = { navController.popBackStack() },
                                    modifier = Modifier
                                )
                            }
                            composable(RootRoute.CatalogError.route) {
                                val startupDestination = splashState.startupDestination as? StartupDestination.CatalogLoadError
                                    ?: return@composable
                                CatalogBootstrapErrorScreen(
                                    error = startupDestination.error,
                                    onRetry = splashViewModel::retryCatalogBootstrap,
                                    onLogout = splashViewModel::logout,
                                    modifier = Modifier
                                )
                            }
                            composable(RootRoute.Preferences.route) {
                                PreferencesScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onOpenAppearance = { navController.navigate(RootRoute.Appearance.route) },
                                    onLoggedOut = { navController.popBackStack() },
                                    showLogout = splashState.startupDestination != StartupDestination.Auth,
                                    modifier = Modifier
                                )
                            }
                            composable(RootRoute.Appearance.route) {
                                AppearanceSettingsScreen(
                                    settingsState = settingsState,
                                    onBackClick = { navController.popBackStack() },
                                    onThemePreferenceChange = settingsViewModel::onThemePreferenceChange,
                                    onDynamicColorChange = settingsViewModel::onDynamicColorChange,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
