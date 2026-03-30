package org.evoionosp.noveliq.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.noveliq.data.audiobook.repository.AudiobookRepositoryImpl
import org.evoionosp.noveliq.data.auth.repository.AuthRepositoryImpl
import org.evoionosp.noveliq.data.library.repository.LibraryRepositoryImpl
import org.evoionosp.noveliq.data.server.repository.ServerRepositoryImpl
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository
import org.evoionosp.noveliq.domain.server.repository.ServerRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindServerRepository(
        impl: ServerRepositoryImpl
    ): ServerRepository

    @Binds
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl
    ): LibraryRepository

    @Binds
    abstract fun bindAudiobookRepository(
        impl: AudiobookRepositoryImpl
    ): AudiobookRepository
}
