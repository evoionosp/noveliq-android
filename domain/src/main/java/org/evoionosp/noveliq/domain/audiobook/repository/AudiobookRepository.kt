package org.evoionosp.noveliq.domain.audiobook.repository

import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus

interface AudiobookRepository {
    fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>>

    fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus>

    suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit>
}
