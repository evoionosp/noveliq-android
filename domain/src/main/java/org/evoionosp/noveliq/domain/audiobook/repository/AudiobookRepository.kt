package org.evoionosp.noveliq.domain.audiobook.repository

import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus

interface AudiobookRepository {
    fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>>

    fun observeAudiobook(libraryId: String, audiobookId: String): Flow<Audiobook?>

    fun observeAudiobookDetail(libraryId: String, audiobookId: String): Flow<AudiobookDetail?>

    fun observeContinueListening(libraryId: String): Flow<List<Audiobook>>

    suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String
    ): DomainResult<Unit>

    fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus>

    suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit>

    suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit>

    /**
     * Fetches the server-side listening progress for an audiobook. Returns
     * [DomainResult.Success] with `null` when the server has no progress recorded yet.
     */
    suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): DomainResult<PlaybackProgress?>

    /**
     * Persists the current listening progress for an audiobook to the server.
     */
    suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress
    ): DomainResult<Unit>
}
