package org.evoionosp.noveliq.domain.catalog.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.auth.usecase.RefreshSessionUseCase
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.usecase.ObserveLibrariesUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.usecase.GetCurrentSessionUseCase

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
 * Use case that encapsulates the complex logic for refreshing the home catalog.
 * 
 * This includes:
 * - Refreshing libraries with auth retry on failure
 * - Refreshing audiobooks and continue listening for the selected library
 * - Handling auth failures by attempting session refresh
 * - Returning appropriate result for UI to handle
 *
 * This use case moves the complex refresh logic out of HomeViewModel, following
 * Clean Architecture principles where ViewModels should be thin and delegate
 * business logic to use cases.
 */
class RefreshHomeCatalogUseCase @Inject constructor(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
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
        val session = getCurrentSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired

        var activeSession = session
        var libraryRefreshResult = refreshLibraries(activeSession)
        
        // Retry with refreshed session on auth failure
        if (libraryRefreshResult.isAuthFailure()) {
            activeSession = refreshSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired
            libraryRefreshResult = refreshLibraries(activeSession)
        }

        val selectedLibraryId = observeSelectedLibraryUseCase().first()?.id
            ?: observeLibrariesUseCase().first().firstOrNull()?.id

        val audiobookRefreshResult = if (selectedLibraryId != null) {
            var result = refreshSelectedLibraryAudiobooks(activeSession, selectedLibraryId)
            if (result.isAuthFailure()) {
                activeSession = refreshSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired
                result = refreshSelectedLibraryAudiobooks(activeSession, selectedLibraryId)
            }
            
            val continueResult = refreshContinueListening(activeSession, selectedLibraryId)
            if (continueResult.isAuthFailure()) {
                activeSession = refreshSessionUseCase() ?: return RefreshHomeCatalogResult.SessionExpired
                refreshContinueListening(activeSession, selectedLibraryId)
            }
            result
        } else {
            DomainResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES)
        }

        return when {
            libraryRefreshResult.isAuthFailure() || audiobookRefreshResult.isAuthFailure() -> {
                // Try one more session refresh
                if (refreshSessionUseCase() == null) {
                    RefreshHomeCatalogResult.SessionExpired
                } else {
                    RefreshHomeCatalogResult.Failure(CatalogError.AUTH)
                }
            }
            libraryRefreshResult is DomainResult.Failure -> {
                RefreshHomeCatalogResult.Failure(libraryRefreshResult.error)
            }
            audiobookRefreshResult is DomainResult.Failure -> {
                RefreshHomeCatalogResult.Failure(audiobookRefreshResult.error)
            }
            else -> RefreshHomeCatalogResult.Success
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
