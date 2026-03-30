package org.evoionosp.noveliq.data.library.local.mapper

import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.SyncStatus

internal fun LibrarySyncStateEntity?.toDomain(): SyncStatus {
    if (this == null) {
        return SyncStatus.Idle
    }

    val error = error?.let { runCatching { CatalogError.valueOf(it) }.getOrDefault(CatalogError.UNKNOWN) }
    return when (status) {
        "SYNCING" -> SyncStatus.Syncing
        "SUCCESS" -> SyncStatus.Success(lastSyncedAtMillis = lastSyncedAtMillis)
        "STALE" -> SyncStatus.Stale(
            lastSyncedAtMillis = lastSyncedAtMillis,
            reason = error ?: CatalogError.UNKNOWN
        )
        "FAILED" -> SyncStatus.Failed(error ?: CatalogError.UNKNOWN)
        else -> SyncStatus.Idle
    }
}
