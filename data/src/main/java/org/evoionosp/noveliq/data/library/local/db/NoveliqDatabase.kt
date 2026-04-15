package org.evoionosp.noveliq.data.library.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.noveliq.data.audiobook.local.entity.AudiobookEntity
import org.evoionosp.noveliq.data.audiobook.local.entity.ContinueListeningEntity
import org.evoionosp.noveliq.data.library.local.dao.LibraryDao
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.entity.LibraryEntity
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity

@Database(
    entities = [
        LibraryEntity::class,
        AudiobookEntity::class,
        ContinueListeningEntity::class,
        LibrarySyncStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NoveliqDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun audiobookDao(): AudiobookDao
    abstract fun continueListeningDao(): ContinueListeningDao
    abstract fun librarySyncStateDao(): LibrarySyncStateDao
}
