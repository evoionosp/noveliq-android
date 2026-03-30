package org.evoionosp.noveliq.data.library.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity

@Dao
interface LibraryDao {
    @Query("SELECT * FROM libraries ORDER BY displayOrder ASC, name ASC")
    fun observeLibraries(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM libraries WHERE isSelected = 1 LIMIT 1")
    fun observeSelectedLibrary(): Flow<LibraryEntity?>

    @Query("SELECT * FROM libraries ORDER BY displayOrder ASC, name ASC")
    suspend fun getLibraries(): List<LibraryEntity>

    @Query("SELECT * FROM libraries WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedLibrary(): LibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraries(libraries: List<LibraryEntity>)

    @Query("UPDATE libraries SET isSelected = 0")
    suspend fun clearSelectedLibrary()

    @Query("UPDATE libraries SET isSelected = CASE WHEN id = :libraryId THEN 1 ELSE 0 END")
    suspend fun selectLibrary(libraryId: String)

    @Query("DELETE FROM libraries WHERE id NOT IN (:libraryIds)")
    suspend fun deleteLibrariesNotIn(libraryIds: List<String>)

    @Query("DELETE FROM libraries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM libraries")
    suspend fun countLibraries(): Int
}
