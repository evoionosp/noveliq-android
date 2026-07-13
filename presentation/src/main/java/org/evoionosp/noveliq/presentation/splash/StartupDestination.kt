package org.evoionosp.noveliq.presentation.splash

import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.library.model.CatalogError

sealed interface StartupDestination {
    data object Auth : StartupDestination

    data class Home(
        val session: LoginSession
    ) : StartupDestination

    data class CatalogLoadError(
        val session: LoginSession,
        val error: CatalogError
    ) : StartupDestination
}
