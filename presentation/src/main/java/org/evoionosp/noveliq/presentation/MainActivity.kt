package org.evoionosp.noveliq.presentation

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.auth.AuthScreen
import org.evoionosp.noveliq.presentation.detail.AudiobookDetailScreen
import org.evoionosp.noveliq.presentation.home.AuthorsScreen
import org.evoionosp.noveliq.presentation.home.HomeScreen
import org.evoionosp.noveliq.presentation.home.LibraryScreen
import org.evoionosp.noveliq.presentation.player.NowPlayingBar
import org.evoionosp.noveliq.presentation.player.NowPlayingOverlay
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
        Library("library"),
        Authors("authors"),
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

    private fun routeBase(route: String?): String? {
        return route?.substringBefore('/')
    }

    private fun rootIndex(route: String?): Int? {
        return when (routeBase(route)) {
            RootRoute.Home.route -> 0
            RootRoute.Library.route -> 1
            RootRoute.Authors.route -> 2
            else -> null
        }
    }

    private fun isAudiobookDetailRoute(route: String?): Boolean {
        return routeBase(route) == RootRoute.AudiobookDetail.route.substringBefore('/')
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.rootEnterTransition(): EnterTransition {
        if (isAudiobookDetailRoute(initialState.destination.route) || isAudiobookDetailRoute(targetState.destination.route)) {
            return EnterTransition.None
        }
        val initialIndex = rootIndex(initialState.destination.route)
        val targetIndex = rootIndex(targetState.destination.route)
        val forward = initialIndex != null && targetIndex != null && targetIndex > initialIndex
        return slideInHorizontally(
            animationSpec = tween(durationMillis = 300),
            initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.rootExitTransition(): ExitTransition {
        if (isAudiobookDetailRoute(initialState.destination.route) || isAudiobookDetailRoute(targetState.destination.route)) {
            return ExitTransition.None
        }
        val initialIndex = rootIndex(initialState.destination.route)
        val targetIndex = rootIndex(targetState.destination.route)
        val forward = initialIndex != null && targetIndex != null && targetIndex > initialIndex
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = 300),
            targetOffsetX = { fullWidth -> if (forward) -fullWidth else fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(durationMillis = 300),
            initialOffsetX = { fullWidth -> fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = 300),
            targetOffsetX = { fullWidth -> -fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnterTransition(): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(durationMillis = 300),
            initialOffsetX = { fullWidth -> -fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.backExitTransition(): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = 300),
            targetOffsetX = { fullWidth -> fullWidth }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.modalEnterTransition(): EnterTransition {
        return slideInVertically(
            animationSpec = tween(durationMillis = 320),
            initialOffsetY = { fullHeight -> fullHeight }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.modalExitTransition(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(durationMillis = 320),
            targetOffsetY = { fullHeight -> fullHeight }
        )
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.modalPopEnterTransition(): EnterTransition {
        return EnterTransition.None
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.modalPopExitTransition(): ExitTransition {
        return slideOutVertically(
            animationSpec = tween(durationMillis = 320),
            targetOffsetY = { fullHeight -> fullHeight }
        )
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
                        var nowPlayingAudiobook by remember {
                            mutableStateOf<org.evoionosp.noveliq.domain.audiobook.model.Audiobook?>(null)
                        }
                        var isNowPlayingExpanded by remember { mutableStateOf(false) }
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route
                        val mainRootRoutes = setOf(
                            RootRoute.Home.route,
                            RootRoute.Library.route,
                            RootRoute.Authors.route
                        )

                        Box {
                            Scaffold(
                                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                bottomBar = {
                                    Column {
                                        if (nowPlayingAudiobook != null && !isNowPlayingExpanded) {
                                            val startupDestination =
                                                splashState.startupDestination as? StartupDestination.Home
                                            NowPlayingBar(
                                                audiobook = nowPlayingAudiobook,
                                                accessToken = startupDestination?.session?.accessToken.orEmpty(),
                                                onExpand = { isNowPlayingExpanded = true }
                                            )
                                        }
                                        if (currentRoute in mainRootRoutes) {
                                            NavigationBar {
                                                listOf(
                                                    Triple(RootRoute.Home, Icons.Rounded.Home, R.string.root_home),
                                                    Triple(RootRoute.Library, Icons.Rounded.AutoStories, R.string.root_library),
                                                    Triple(RootRoute.Authors, Icons.Rounded.Group, R.string.root_authors)
                                                ).forEach { (route, icon, labelRes) ->
                                                    NavigationBarItem(
                                                        selected = currentRoute == route.route,
                                                        onClick = {
                                                            navController.navigate(route.route) {
                                                                popUpTo(RootRoute.Home.route) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        },
                                                        icon = { Icon(imageVector = icon, contentDescription = null) },
                                                        label = { Text(text = getString(labelRes)) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = baseRoute.route
                                ) {
                                composable(
                                    route = RootRoute.Auth.route,
                                    enterTransition = { backEnterTransition() },
                                    exitTransition = { forwardExitTransition() },
                                    popEnterTransition = { backEnterTransition() },
                                    popExitTransition = { backExitTransition() }
                                ) {
                                    AuthScreen(
                                        modifier = Modifier,
                                        onOpenSettings = { navController.navigate(RootRoute.Preferences.route) }
                                    )
                                }
                                composable(
                                    route = RootRoute.Home.route,
                                    enterTransition = { rootEnterTransition() },
                                    exitTransition = { rootExitTransition() },
                                    popEnterTransition = { rootEnterTransition() },
                                    popExitTransition = { rootExitTransition() }
                                ) {
                                    val startupDestination = splashState.startupDestination as? StartupDestination.Home
                                        ?: return@composable
                                    HomeScreen(
                                        accessToken = startupDestination.session.accessToken,
                                        onOpenSettings = { navController.navigate(RootRoute.Preferences.route) },
                                        onSessionExpired = {
                                            navController.navigate(RootRoute.Auth.route) {
                                                popUpTo(0)
                                            }
                                        },
                                        bottomBarPadding = innerPadding.calculateBottomPadding(),
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
                                composable(
                                    route = RootRoute.Library.route,
                                    enterTransition = { rootEnterTransition() },
                                    exitTransition = { rootExitTransition() },
                                    popEnterTransition = { rootEnterTransition() },
                                    popExitTransition = { rootExitTransition() }
                                ) {
                                    val startupDestination = splashState.startupDestination as? StartupDestination.Home
                                        ?: return@composable
                                    LibraryScreen(
                                        accessToken = startupDestination.session.accessToken,
                                        onOpenSettings = { navController.navigate(RootRoute.Preferences.route) },
                                        onSessionExpired = {
                                            navController.navigate(RootRoute.Auth.route) {
                                                popUpTo(0)
                                            }
                                        },
                                        bottomBarPadding = innerPadding.calculateBottomPadding(),
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
                                composable(
                                    route = RootRoute.Authors.route,
                                    enterTransition = { rootEnterTransition() },
                                    exitTransition = { rootExitTransition() },
                                    popEnterTransition = { rootEnterTransition() },
                                    popExitTransition = { rootExitTransition() }
                                ) {
                                    val startupDestination = splashState.startupDestination as? StartupDestination.Home
                                        ?: return@composable
                                    AuthorsScreen(
                                        accessToken = startupDestination.session.accessToken,
                                        onOpenSettings = { navController.navigate(RootRoute.Preferences.route) },
                                        onSessionExpired = {
                                            navController.navigate(RootRoute.Auth.route) {
                                                popUpTo(0)
                                            }
                                        },
                                        bottomBarPadding = innerPadding.calculateBottomPadding(),
                                        modifier = Modifier
                                    )
                                }
                                composable(
                                    route = RootRoute.AudiobookDetail.route,
                                    enterTransition = { modalEnterTransition() },
                                    exitTransition = { modalExitTransition() },
                                    popEnterTransition = { modalPopEnterTransition() },
                                    popExitTransition = { modalPopExitTransition() }
                                ) { backStackEntry ->
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
                                        onPlayClick = { audiobook ->
                                            nowPlayingAudiobook = audiobook
                                            isNowPlayingExpanded = true
                                            navController.popBackStack()
                                        },
                                        modifier = Modifier
                                    )
                                }
                                composable(
                                    route = RootRoute.CatalogError.route,
                                    enterTransition = { forwardEnterTransition() },
                                    exitTransition = { forwardExitTransition() },
                                    popEnterTransition = { backEnterTransition() },
                                    popExitTransition = { backExitTransition() }
                                ) {
                                    val startupDestination = splashState.startupDestination as? StartupDestination.CatalogLoadError
                                        ?: return@composable
                                    CatalogBootstrapErrorScreen(
                                        error = startupDestination.error,
                                        onRetry = splashViewModel::retryCatalogBootstrap,
                                        onLogout = splashViewModel::logout,
                                        modifier = Modifier
                                    )
                                }
                                composable(
                                    route = RootRoute.Preferences.route,
                                    enterTransition = { forwardEnterTransition() },
                                    exitTransition = { forwardExitTransition() },
                                    popEnterTransition = { backEnterTransition() },
                                    popExitTransition = { backExitTransition() }
                                ) {
                                    PreferencesScreen(
                                        onBackClick = { navController.popBackStack() },
                                        onOpenAppearance = { navController.navigate(RootRoute.Appearance.route) },
                                        onLoggedOut = {
                                            navController.navigate(RootRoute.Auth.route) {
                                                popUpTo(0)
                                            }
                                        },
                                        showLogout = splashState.startupDestination != StartupDestination.Auth,
                                        modifier = Modifier
                                    )
                                }
                                composable(
                                    route = RootRoute.Appearance.route,
                                    enterTransition = { forwardEnterTransition() },
                                    exitTransition = { forwardExitTransition() },
                                    popEnterTransition = { backEnterTransition() },
                                    popExitTransition = { backExitTransition() }
                                ) {
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

                            val startupDestination =
                                splashState.startupDestination as? StartupDestination.Home
                            NowPlayingOverlay(
                                visible = isNowPlayingExpanded,
                                audiobook = nowPlayingAudiobook,
                                accessToken = startupDestination?.session?.accessToken.orEmpty(),
                                onMinimize = { isNowPlayingExpanded = false },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
