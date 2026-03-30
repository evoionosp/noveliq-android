package org.evoionosp.noveliq.domain.library.model

sealed interface BootstrapHomeCatalogResult {
    data class Success(
        val selectedLibraryId: String,
        val usedCachedData: Boolean
    ) : BootstrapHomeCatalogResult

    data object NoLibrariesAvailable : BootstrapHomeCatalogResult

    data class Failure(val error: CatalogError) : BootstrapHomeCatalogResult
}
