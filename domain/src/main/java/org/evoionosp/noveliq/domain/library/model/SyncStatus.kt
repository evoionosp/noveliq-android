package org.evoionosp.noveliq.domain.library.model

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val lastSyncedAtMillis: Long?) : SyncStatus
    data class Stale(
        val lastSyncedAtMillis: Long?,
        val reason: CatalogError
    ) : SyncStatus
    data class Failed(val reason: CatalogError) : SyncStatus
}
