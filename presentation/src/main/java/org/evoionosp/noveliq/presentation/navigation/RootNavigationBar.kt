package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.player.NowPlayingBar

private data class RootNavItem(
    val route: AppRoute,
    val icon: ImageVector,
    val labelResId: Int
)

private val rootNavItems = listOf(
    RootNavItem(AppRoute.Home, Icons.Rounded.Home, R.string.root_home),
    RootNavItem(AppRoute.Library, Icons.Rounded.AutoStories, R.string.root_library),
    RootNavItem(AppRoute.Authors, Icons.Rounded.Group, R.string.root_authors)
)

@Composable
internal fun RootNavigationBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    nowPlayingAudiobook: Audiobook?,
    nowPlayingAccessToken: String,
    isNowPlayingExpanded: Boolean,
    onExpandNowPlaying: () -> Unit
) {
    Column {
        if (nowPlayingAudiobook != null && !isNowPlayingExpanded) {
            NowPlayingBar(
                audiobook = nowPlayingAudiobook,
                accessToken = nowPlayingAccessToken,
                onExpand = onExpandNowPlaying
            )
        }
        if (currentRoute in mainRootRoutes) {
            NavigationBar {
                rootNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route.route,
                        onClick = {
                            navController.navigate(item.route.route) {
                                popUpTo(AppRoute.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = null) },
                        label = { Text(text = stringResource(item.labelResId)) }
                    )
                }
            }
        }
    }
}
