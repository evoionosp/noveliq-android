package org.evoionosp.noveliq.domain.library.repository

import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.DomainResult

interface LibraryRepository {
    fun observeLibraries(): Flow<List<AudiobookLibrary>>

    fun observeSelectedLibrary(): Flow<AudiobookLibrary?>

    suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String
    ): DomainResult<Unit>

    suspend fun selectLibrary(libraryId: String): DomainResult<Unit>
}
