package org.evoionosp.noveliq.presentation.navigation

import android.net.Uri

internal enum class AppRoute(val route: String) {
    Auth("auth"),
    Home("home"),
    Library("library"),
    Authors("authors"),
    AudiobookDetail("audiobook/{libraryId}/{audiobookId}"),
    CatalogError("catalog_error"),
    Preferences("preferences"),
    Appearance("appearance")
}

internal fun audiobookDetailRoute(libraryId: String, audiobookId: String): String {
    return "audiobook/${Uri.encode(libraryId)}/${Uri.encode(audiobookId)}"
}

internal val mainRootRoutes = setOf(
    AppRoute.Home.route,
    AppRoute.Library.route,
    AppRoute.Authors.route
)
