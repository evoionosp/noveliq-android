package org.evoionosp.noveliq.catalog

import javax.inject.Named
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.core.session.SessionStore
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshContinueListeningUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.RefreshSelectedLibraryAudiobooksUseCase
import org.evoionosp.noveliq.domain.library.usecase.ObserveSelectedLibraryUseCase
import org.evoionosp.noveliq.domain.library.usecase.RefreshLibrariesUseCase

@Singleton
class CatalogSyncCoordinator @Inject constructor(
    @param:Named("application_scope") private val scope: CoroutineScope,
    private val sessionStore: SessionStore,
    private val connectivityObserver: ConnectivityObserver,
    private val observeSelectedLibraryUseCase: ObserveSelectedLibraryUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val refreshContinueListeningUseCase: RefreshContinueListeningUseCase,
    private val refreshSelectedLibraryAudiobooksUseCase: RefreshSelectedLibraryAudiobooksUseCase
) {
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            connectivityObserver.observe()
                .distinctUntilChanged()
                .collect { isConnected ->
                    if (isConnected) {
                        syncCurrentSelection()
                    }
                }
        }

        scope.launch {
            observeSelectedLibraryUseCase()
                .filterNotNull()
                .collect { library ->
                    val session = sessionStore.session.first() ?: return@collect
                    if (connectivityObserver.isConnected()) {
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
                }
        }
    }

    private suspend fun syncCurrentSelection() {
        val session = sessionStore.session.first() ?: return
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
