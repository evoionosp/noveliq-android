package org.evoionosp.noveliq.data.library.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity

@Dao
interface LibrarySyncStateDao {
    @Query("SELECT * FROM library_sync_state WHERE libraryId = :libraryId LIMIT 1")
    fun observeSyncState(libraryId: String): Flow<LibrarySyncStateEntity?>

    @Query("SELECT * FROM library_sync_state WHERE libraryId = :libraryId LIMIT 1")
    suspend fun getSyncState(libraryId: String): LibrarySyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syncState: LibrarySyncStateEntity)

    @Query("DELETE FROM library_sync_state WHERE libraryId NOT IN (:libraryIds)")
    suspend fun deleteByLibrariesNotIn(libraryIds: List<String>)

    @Query("DELETE FROM library_sync_state")
    suspend fun deleteAll()
}
