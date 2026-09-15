package org.evoionosp.noveliq.data.audiobook.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.ContinueListeningEntity

/**
 * A continue-listening row: the cached book plus the last reported playback position.
 */
data class ContinueListeningItem(
    @Embedded val audiobook: AudiobookEntity,
    @ColumnInfo(name = "currentTimeSeconds") val currentTimeSeconds: Double?,
)

@Dao
interface ContinueListeningDao {
    @Query(
        """
        SELECT audiobooks.*, continue_listening_items.currentTimeSeconds
        FROM continue_listening_items
        INNER JOIN audiobooks ON audiobooks.id = continue_listening_items.audiobookId
        WHERE continue_listening_items.libraryId = :libraryId
        ORDER BY continue_listening_items.progressLastUpdateMillis DESC
        """,
    )
    fun observeContinueListening(libraryId: String): Flow<List<ContinueListeningItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<ContinueListeningEntity>)

    @Query("DELETE FROM continue_listening_items WHERE libraryId = :libraryId")
    suspend fun deleteByLibraryId(libraryId: String)

    @Query("DELETE FROM continue_listening_items")
    suspend fun deleteAll()
}
