package org.evoionosp.noveliq.domain.catalog.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase

/**
 * Result of a home catalog refresh operation.
 */
sealed class RefreshHomeCatalogResult {
    /** Refresh completed successfully. */
    data object Success : RefreshHomeCatalogResult()

    /** Session expired and could not be refreshed. User should be logged out. */
    data object SessionExpired : RefreshHomeCatalogResult()

    /** Refresh failed with a specific error. */
    data class Failure(val error: CatalogError) : RefreshHomeCatalogResult()
}

/**
 * Refreshes the home catalog: libraries, then the selected library's audiobooks and continue
 * listening shelf.
 *
 * This use case no longer retries auth failures itself. Token rotation happens one layer down, in
 * the HTTP client's authenticator, so by the time an AUTH error surfaces here the refresh has
 * already been tried and the session has already been cleared — the only thing left to do is tell
 * the caller to send the user to login.
 */
class RefreshHomeCatalogUseCase @Inject constructor(
    private val getValidSessionUseCase: GetValidSessionUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
    private val observeLibrariesUseCase: ObserveLibrariesUseCase,
    private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
    private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase
) {
    /**
     * Performs a full refresh of the home catalog.
     *
     * @return RefreshHomeCatalogResult indicating success, session expiration, or failure
     */
    suspend operator fun invoke(): RefreshHomeCatalogResult {
        val session = getValidSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired

        val libraryRefreshResult = refreshLibraries(session)
        if (libraryRefreshResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired

        val selectedLibraryId = observeSelectedLibraryUseCase().first()?.id
            ?: observeLibrariesUseCase().first().firstOrNull()?.id
            ?: return resultFor(libraryRefreshResult, CatalogError.NO_AUDIOBOOK_LIBRARIES)

        val audiobookRefreshResult = refreshSelectedLibraryAudiobooks(session, selectedLibraryId)
        if (audiobookRefreshResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired

        val continueListeningResult = refreshContinueListening(session, selectedLibraryId)
        if (continueListeningResult.isAuthFailure()) return RefreshHomeCatalogResult.SessionExpired

        return when {
            libraryRefreshResult is DomainResult.Failure ->
                RefreshHomeCatalogResult.Failure(libraryRefreshResult.error)
            audiobookRefreshResult is DomainResult.Failure ->
                RefreshHomeCatalogResult.Failure(audiobookRefreshResult.error)
            else -> RefreshHomeCatalogResult.Success
        }
    }

    private fun resultFor(
        libraryRefreshResult: DomainResult<Unit>,
        fallbackError: CatalogError
    ): RefreshHomeCatalogResult {
        return if (libraryRefreshResult is DomainResult.Failure) {
            RefreshHomeCatalogResult.Failure(libraryRefreshResult.error)
        } else {
            RefreshHomeCatalogResult.Failure(fallbackError)
        }
    }

    private suspend fun refreshLibraries(session: LoginSession): DomainResult<Unit> {
        return refreshLibrariesUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken
        )
    }

    private suspend fun refreshSelectedLibraryAudiobooks(
        session: LoginSession,
        libraryId: String
    ): DomainResult<Unit> {
        return refreshSelectedLibraryAudiobooksUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            libraryId = libraryId
        )
    }

    private suspend fun refreshContinueListening(
        session: LoginSession,
        libraryId: String
    ): DomainResult<Unit> {
        return refreshContinueListeningUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken,
            libraryId = libraryId
        )
    }

    private fun DomainResult<Unit>.isAuthFailure(): Boolean {
        return this is DomainResult.Failure && error == CatalogError.AUTH
    }
}
