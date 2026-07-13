package org.evoionosp.noveliq.presentation.navigation

internal enum class AppRoute(val route: String) {
    Auth("auth"),
    Home("home"),
    Library("library"),
    CatalogError("catalog_error"),
    Preferences("preferences"),
    Appearance("appearance")
}

internal val mainRootRoutes = setOf(
    AppRoute.Home.route,
    AppRoute.Library.route
)
