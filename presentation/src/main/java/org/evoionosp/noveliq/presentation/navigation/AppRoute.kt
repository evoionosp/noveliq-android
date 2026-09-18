package org.evoionosp.noveliq.presentation.navigation

internal const val BOOK_DETAILS_ARG_LIBRARY_ID = "libraryId"
internal const val BOOK_DETAILS_ARG_AUDIOBOOK_ID = "audiobookId"

internal enum class AppRoute(
    val route: String,
) {
    Auth("auth"),
    Home("home"),
    Library("library"),
    CatalogError("catalog_error"),
    Preferences("preferences"),
    Appearance("appearance"),
    BookDetails("book_details/{$BOOK_DETAILS_ARG_LIBRARY_ID}/{$BOOK_DETAILS_ARG_AUDIOBOOK_ID}"),
}

/** Navigable route for one book's standalone details page. */
internal fun bookDetailsRoute(
    libraryId: String,
    audiobookId: String,
): String = "book_details/$libraryId/$audiobookId"

internal val mainRootRoutes =
    setOf(
        AppRoute.Home.route,
        AppRoute.Library.route,
    )
