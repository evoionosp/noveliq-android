package org.evoionosp.noveliq.domain.library.repository

interface LibraryRepository {
    suspend fun loadLibrary(
        baseUrl: String,
        accessToken: String
    ): Boolean
}
