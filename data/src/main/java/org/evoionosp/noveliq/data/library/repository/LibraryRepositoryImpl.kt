package org.evoionosp.noveliq.data.library.repository

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository

@Singleton
class LibraryRepositoryImpl @Inject constructor() : LibraryRepository {
    override suspend fun loadLibrary(
        baseUrl: String,
        accessToken: String
    ): Boolean {
        return true
    }
}
