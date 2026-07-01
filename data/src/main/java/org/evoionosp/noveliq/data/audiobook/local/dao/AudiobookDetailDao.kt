package org.evoionosp.noveliq.data.audiobook.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookChapterEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookDetailEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookTrackEntity

@Dao
interface AudiobookDetailDao {
    @Query("SELECT * FROM audiobook_details WHERE libraryId = :libraryId AND audiobookId = :audiobookId LIMIT 1")
    fun observeDetail(libraryId: String, audiobookId: String): Flow<AudiobookDetailEntity?>

    @Query("SELECT * FROM audiobook_chapters WHERE audiobookId = :audiobookId ORDER BY chapterIndex ASC")
    fun observeChapters(audiobookId: String): Flow<List<AudiobookChapterEntity>>

    @Query("SELECT * FROM audiobook_tracks WHERE audiobookId = :audiobookId ORDER BY trackIndex ASC")
    fun observeTracks(audiobookId: String): Flow<List<AudiobookTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetail(detail: AudiobookDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(chapters: List<AudiobookChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<AudiobookTrackEntity>)

    @Query("DELETE FROM audiobook_chapters WHERE audiobookId = :audiobookId")
    suspend fun deleteChapters(audiobookId: String)

    @Query("DELETE FROM audiobook_tracks WHERE audiobookId = :audiobookId")
    suspend fun deleteTracks(audiobookId: String)
}
