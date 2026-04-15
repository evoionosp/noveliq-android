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

    @Provides
    @Singleton
    fun provideNoveliqDatabase(
        @ApplicationContext context: Context
    ): NoveliqDatabase {
        return Room.databaseBuilder(
            context,
            NoveliqDatabase::class.java,
            "noveliq.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    fun provideLibraryDao(database: NoveliqDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideAudiobookDao(database: NoveliqDatabase): AudiobookDao = database.audiobookDao()

    @Provides
    fun provideContinueListeningDao(database: NoveliqDatabase): ContinueListeningDao {
        return database.continueListeningDao()
    }

    @Provides
    fun provideLibrarySyncStateDao(database: NoveliqDatabase): LibrarySyncStateDao {
        return database.librarySyncStateDao()
    }
}
