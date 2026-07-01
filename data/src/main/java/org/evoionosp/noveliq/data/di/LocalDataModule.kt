package org.evoionosp.noveliq.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.noveliq.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.noveliq.data.connectivity.AndroidConnectivityObserver
import org.evoionosp.noveliq.data.library.local.dao.LibraryDao
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(
        impl: AndroidConnectivityObserver
    ): ConnectivityObserver
}

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `continue_listening_items` (
                    `audiobookId` TEXT NOT NULL,
                    `libraryId` TEXT NOT NULL,
                    `progressLastUpdateMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`audiobookId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_continue_listening_items_libraryId` ON `continue_listening_items` (`libraryId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_continue_listening_items_libraryId_progressLastUpdateMillis` ON `continue_listening_items` (`libraryId`, `progressLastUpdateMillis`)"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `audiobook_details` (
                    `audiobookId` TEXT NOT NULL,
                    `libraryId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `author` TEXT NOT NULL,
                    `coverUrl` TEXT NOT NULL,
                    `series` TEXT,
                    `durationInSeconds` INTEGER,
                    `description` TEXT,
                    `refreshedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`audiobookId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_audiobook_details_libraryId` ON `audiobook_details` (`libraryId`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `audiobook_chapters` (
                    `audiobookId` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `startInSeconds` INTEGER NOT NULL,
                    `endInSeconds` INTEGER,
                    PRIMARY KEY(`audiobookId`, `chapterIndex`),
                    FOREIGN KEY(`audiobookId`) REFERENCES `audiobook_details`(`audiobookId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_audiobook_chapters_audiobookId` ON `audiobook_chapters` (`audiobookId`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `audiobook_tracks` (
                    `audiobookId` TEXT NOT NULL,
                    `trackIndex` INTEGER NOT NULL,
                    `startOffsetInSeconds` INTEGER NOT NULL,
                    `durationInSeconds` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `remoteUrl` TEXT NOT NULL,
                    `mimeType` TEXT,
                    PRIMARY KEY(`audiobookId`, `trackIndex`),
                    FOREIGN KEY(`audiobookId`) REFERENCES `audiobook_details`(`audiobookId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_audiobook_tracks_audiobookId` ON `audiobook_tracks` (`audiobookId`)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideNoveliqDatabase(
        @ApplicationContext context: Context
    ): NoveliqDatabase {
        return Room.databaseBuilder(
            context,
            NoveliqDatabase::class.java,
            "noveliq.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    fun provideLibraryDao(database: NoveliqDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideAudiobookDao(database: NoveliqDatabase): AudiobookDao = database.audiobookDao()

    @Provides
    fun provideAudiobookDetailDao(database: NoveliqDatabase): AudiobookDetailDao {
        return database.audiobookDetailDao()
    }

    @Provides
    fun provideContinueListeningDao(database: NoveliqDatabase): ContinueListeningDao {
        return database.continueListeningDao()
    }

    @Provides
    fun provideLibrarySyncStateDao(database: NoveliqDatabase): LibrarySyncStateDao {
        return database.librarySyncStateDao()
    }
}
