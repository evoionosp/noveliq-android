package org.evoionosp.noveliq.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.utils.SheetDragState

private data class RootNavItem(
    val route: AppRoute,
    val icon: ImageVector,
    val labelResId: Int,
)

private val rootNavItems =
    listOf(
        RootNavItem(AppRoute.Home, Icons.Rounded.Home, R.string.root_home),
        RootNavItem(AppRoute.Library, Icons.Rounded.AutoStories, R.string.root_library),
    )

/**
 * Root tab bar. The mini player used to live above this bar; it now renders
 * inside the unified Now Playing sheet, so this stays a constant-height nav.
 */
@Composable
internal fun RootNavigationBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    sheetDrag: SheetDragState,
) {
    if (currentRoute in mainRootRoutes) {
        // Driven continuously by the sheet fraction (draw phase, no
        // recomposition): slides down and fades as the player expands,
        // returns as it minimizes. Scaffold keeps reserving the space.
        NavigationBar(
            modifier =
                Modifier.graphicsLayer {
                    val fraction = sheetDrag.expansionFraction
                    translationY = fraction * size.height
                    alpha = (1f - fraction * 2f).coerceIn(0f, 1f)
                },
        ) {
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
                    label = { Text(text = stringResource(item.labelResId)) },
                )
            }
        }
    }
}
