package org.evoionosp.noveliq.catalog

import javax.inject.Named
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.catalog.usecase.ObserveAndSyncCatalogUseCase

@Singleton
class CatalogSyncCoordinator @Inject constructor(
    @param:Named("application_scope") private val scope: CoroutineScope,
    private val observeAndSyncCatalogUseCase: ObserveAndSyncCatalogUseCase
) {
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            observeAndSyncCatalogUseCase().collect()
        }
    }
}
