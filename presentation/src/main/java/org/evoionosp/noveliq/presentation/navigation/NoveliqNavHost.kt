package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.auth.AuthScreen
import org.evoionosp.noveliq.presentation.home.AuthorsScreen
import org.evoionosp.noveliq.presentation.home.HomeScreen
import org.evoionosp.noveliq.presentation.home.LibraryScreen
import org.evoionosp.noveliq.presentation.settings.AppearanceSettingsScreen
import org.evoionosp.noveliq.presentation.settings.PreferencesScreen
import org.evoionosp.noveliq.presentation.settings.SettingsUiState
import org.evoionosp.noveliq.presentation.splash.CatalogBootstrapErrorScreen
import org.evoionosp.noveliq.presentation.splash.SplashUiState
import org.evoionosp.noveliq.presentation.splash.StartupDestination
import org.evoionosp.noveliq.presentation.ui.theme.ThemePreference

@Composable
internal fun NoveliqNavHost(
    navController: NavHostController,
    startDestination: String,
    splashState: SplashUiState,
    settingsState: SettingsUiState,
    bottomBarPadding: Dp,
    onRetryCatalogBootstrap: () -> Unit,
    onLogout: () -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenAudiobook: (Audiobook) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = AppRoute.Auth.route,
            enterTransition = { backEnterTransition() },
            exitTransition = { forwardExitTransition() },
            popEnterTransition = { backEnterTransition() },
            popExitTransition = { backExitTransition() }
        ) {
            AuthScreen(
                modifier = Modifier,
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) }
            )
        }
        composable(
            route = AppRoute.Home.route,
            enterTransition = { rootEnterTransition() },
            exitTransition = { rootExitTransition() },
            popEnterTransition = { rootEnterTransition() },
            popExitTransition = { rootExitTransition() }
        ) {
            val startupDestination = splashState.startupDestination as? StartupDestination.Home
                ?: return@composable
            HomeScreen(
                accessToken = startupDestination.session.accessToken,
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onSessionExpired = { navController.navigateToAuthRoot() },
                bottomBarPadding = bottomBarPadding,
                onOpenAudiobook = onOpenAudiobook,
                modifier = Modifier
            )
        }
        composable(
            route = AppRoute.Library.route,
            enterTransition = { rootEnterTransition() },
            exitTransition = { rootExitTransition() },
            popEnterTransition = { rootEnterTransition() },
            popExitTransition = { rootExitTransition() }
        ) {
            val startupDestination = splashState.startupDestination as? StartupDestination.Home
                ?: return@composable
            LibraryScreen(
                accessToken = startupDestination.session.accessToken,
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onSessionExpired = { navController.navigateToAuthRoot() },
                bottomBarPadding = bottomBarPadding,
                onOpenAudiobook = onOpenAudiobook,
                modifier = Modifier
            )
        }
        composable(
            route = AppRoute.Authors.route,
            enterTransition = { rootEnterTransition() },
            exitTransition = { rootExitTransition() },
            popEnterTransition = { rootEnterTransition() },
            popExitTransition = { rootExitTransition() }
        ) {
            val startupDestination = splashState.startupDestination as? StartupDestination.Home
                ?: return@composable
            AuthorsScreen(
                accessToken = startupDestination.session.accessToken,
                onOpenSettings = { navController.navigate(AppRoute.Preferences.route) },
                onSessionExpired = { navController.navigateToAuthRoot() },
                bottomBarPadding = bottomBarPadding,
                modifier = Modifier
            )
        }
        composable(
            route = AppRoute.CatalogError.route,
            enterTransition = { forwardEnterTransition() },
            exitTransition = { forwardExitTransition() },
            popEnterTransition = { backEnterTransition() },
            popExitTransition = { backExitTransition() }
        ) {
            val startupDestination = splashState.startupDestination as? StartupDestination.CatalogLoadError
                ?: return@composable
            CatalogBootstrapErrorScreen(
                error = startupDestination.error,
                onRetry = onRetryCatalogBootstrap,
                onLogout = onLogout,
                modifier = Modifier
            )
        }
        composable(
            route = AppRoute.Preferences.route,
            enterTransition = { forwardEnterTransition() },
            exitTransition = { forwardExitTransition() },
            popEnterTransition = { backEnterTransition() },
            popExitTransition = { backExitTransition() }
        ) {
            PreferencesScreen(
                onBackClick = { navController.popBackStack() },
                onOpenAppearance = { navController.navigate(AppRoute.Appearance.route) },
                onLoggedOut = { navController.navigateToAuthRoot() },
                showLogout = splashState.startupDestination != StartupDestination.Auth,
                modifier = Modifier
            )
        }
        composable(
            route = AppRoute.Appearance.route,
            enterTransition = { forwardEnterTransition() },
            exitTransition = { forwardExitTransition() },
            popEnterTransition = { backEnterTransition() },
            popExitTransition = { backExitTransition() }
        ) {
            AppearanceSettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.popBackStack() },
                onThemePreferenceChange = onThemePreferenceChange,
                onDynamicColorChange = onDynamicColorChange,
                modifier = Modifier
            )
        }
    }
}

private fun NavHostController.navigateToAuthRoot() {
    navigate(AppRoute.Auth.route) {
        popUpTo(0)
    }
}
