package org.evoionosp.noveliq.data.library.local.mapper

import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStateMappersTest {
    @Test
    fun `null state maps to idle`() {
        assertEquals(SyncStatus.Idle, (null as LibrarySyncStateEntity?).toDomain())
    }

    @Test
    fun `syncing maps to syncing`() {
        assertEquals(
            SyncStatus.Syncing,
            state(status = "SYNCING").toDomain(),
        )
    }

    @Test
    fun `success carries the last synced time`() {
        assertEquals(
            SyncStatus.Success(lastSyncedAtMillis = 123L),
            state(status = "SUCCESS", lastSyncedAtMillis = 123L).toDomain(),
        )
    }

    @Test
    fun `stale carries time and parsed reason`() {
        assertEquals(
            SyncStatus.Stale(lastSyncedAtMillis = 7L, reason = CatalogError.NETWORK),
            state(status = "STALE", lastSyncedAtMillis = 7L, error = "NETWORK").toDomain(),
        )
    }

    @Test
    fun `stale falls back to unknown for missing or unrecognized reasons`() {
        assertEquals(
            SyncStatus.Stale(lastSyncedAtMillis = null, reason = CatalogError.UNKNOWN),
            state(status = "STALE", error = null).toDomain(),
        )
        assertEquals(
            SyncStatus.Stale(lastSyncedAtMillis = null, reason = CatalogError.UNKNOWN),
            state(status = "STALE", error = "SOMETHING_NEW").toDomain(),
        )
    }

    @Test
    fun `failed carries the parsed error`() {
        assertEquals(
            SyncStatus.Failed(CatalogError.AUTH),
            state(status = "FAILED", error = "AUTH").toDomain(),
        )
        assertEquals(
            SyncStatus.Failed(CatalogError.UNKNOWN),
            state(status = "FAILED", error = null).toDomain(),
        )
    }

    @Test
    fun `unrecognized status maps to idle`() {
        assertEquals(SyncStatus.Idle, state(status = "WHATEVER").toDomain())
    }

    private fun state(
        status: String,
        lastSyncedAtMillis: Long? = null,
        error: String? = null,
    ): LibrarySyncStateEntity =
        LibrarySyncStateEntity(
            libraryId = "lib1",
            status = status,
            lastSyncedAtMillis = lastSyncedAtMillis,
            error = error,
        )
}
