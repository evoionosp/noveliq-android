package org.evoionosp.noveliq.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.noveliq.data.auth.repository.AuthRepositoryImpl
import org.evoionosp.noveliq.data.server.repository.ServerRepositoryImpl
import org.evoionosp.noveliq.domain.auth.AuthRepository
import org.evoionosp.noveliq.domain.server.ServerRepository

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
}
