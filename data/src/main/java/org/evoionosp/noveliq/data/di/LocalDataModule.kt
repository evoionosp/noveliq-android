package org.evoionosp.noveliq.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.connectivity.AndroidConnectivityObserver
import org.evoionosp.noveliq.data.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.data.library.local.dao.LibraryDao
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase

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
    @Provides
    @Singleton
    fun provideNoveliqDatabase(
        @ApplicationContext context: Context
    ): NoveliqDatabase {
        return Room.databaseBuilder(
            context,
            NoveliqDatabase::class.java,
            "noveliq.db"
        ).build()
    }

    @Provides
    fun provideLibraryDao(database: NoveliqDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideAudiobookDao(database: NoveliqDatabase): AudiobookDao = database.audiobookDao()

    @Provides
    fun provideLibrarySyncStateDao(database: NoveliqDatabase): LibrarySyncStateDao {
        return database.librarySyncStateDao()
    }
}
