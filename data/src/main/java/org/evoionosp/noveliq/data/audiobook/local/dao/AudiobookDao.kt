package org.evoionosp.noveliq.data.audiobook.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity

@Dao
interface AudiobookDao {
    @Query("SELECT * FROM audiobooks WHERE libraryId = :libraryId ORDER BY title COLLATE NOCASE ASC")
    fun observeAudiobooks(libraryId: String): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE libraryId = :libraryId AND id = :audiobookId LIMIT 1")
    fun observeAudiobook(libraryId: String, audiobookId: String): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE libraryId = :libraryId ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAudiobooks(libraryId: String): List<AudiobookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAudiobooks(audiobooks: List<AudiobookEntity>)

    @Query("DELETE FROM audiobooks WHERE libraryId = :libraryId")
    suspend fun deleteByLibraryId(libraryId: String)

    @Query("DELETE FROM audiobooks WHERE libraryId NOT IN (:libraryIds)")
    suspend fun deleteByLibrariesNotIn(libraryIds: List<String>)

    @Query("DELETE FROM audiobooks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM audiobooks WHERE libraryId = :libraryId")
    suspend fun countByLibraryId(libraryId: String): Int
}
