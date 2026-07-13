package org.evoionosp.noveliq.domain.catalog.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase
import org.evoionosp.noveliq.domain.session.usecase.GetCurrentSessionUseCase

/**
 * Use case that coordinates catalog synchronization based on connectivity and library selection.
 * 
 * This use case encapsulates the business logic for when and what to sync:
 * - When connectivity is restored, sync the current selection (libraries, audiobooks, continue listening)
 * - When the selected library changes and device is connected, refresh audiobooks and continue listening
 *
 * The use case returns a Flow that emits Unit when sync operations are triggered. The caller
 * should collect this flow to activate the synchronization logic.
 */
class ObserveAndSyncCatalogUseCase @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase,
    private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase
) {
    /**
     * Returns a Flow that triggers catalog synchronization based on connectivity and library selection.
     * Collect this flow to activate the sync coordination logic.
     */
    operator fun invoke(): Flow<Unit> {
        // Sync when connectivity is restored
        val connectivitySync = connectivityObserver.observe()
            .distinctUntilChanged()
            .flatMapLatest { isConnected ->
                if (isConnected) {
                    flowOf(Unit)
                } else {
                    flowOf()
                }
            }
            .map {
                syncCurrentSelection()
            }

        // Sync when selected library changes and device is connected
        val libraryChangeSync = observeSelectedLibraryUseCase()
            .filterNotNull()
            .flatMapLatest { library ->
                if (connectivityObserver.isConnected()) {
                    flowOf(library)
                } else {
                    flowOf()
                }
            }
            .map { library ->
                val session = getCurrentSessionUseCase() ?: return@map
                refreshSelectedLibraryAudiobooksUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = library.id
                )
                refreshContinueListeningUseCase(
                    baseUrl = session.baseUrl,
                    accessToken = session.accessToken,
                    libraryId = library.id
                )
            }

        return combine(connectivitySync, libraryChangeSync) { _, _ -> Unit }
    }

    private suspend fun syncCurrentSelection() {
        val session = getCurrentSessionUseCase() ?: return
        refreshLibrariesUseCase(
            baseUrl = session.baseUrl,
            accessToken = session.accessToken
        )

        val selectedLibrary = observeSelectedLibraryUseCase().first()
        if (selectedLibrary != null) {
            refreshSelectedLibraryAudiobooksUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = selectedLibrary.id
            )
            refreshContinueListeningUseCase(
                baseUrl = session.baseUrl,
                accessToken = session.accessToken,
                libraryId = selectedLibrary.id
            )
        }
    }
}
