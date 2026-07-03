package org.evoionosp.noveliq.data.audiobook.repository

import android.util.Log
import androidx.room.withTransaction
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDetailDao
import org.evoionosp.noveliq.data.audiobook.local.dao.ContinueListeningDao
import org.evoionosp.noveliq.data.audiobook.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.data.library.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.library.remote.mapper.toChapterEntities
import org.evoionosp.noveliq.data.library.remote.mapper.toDetailEntity
import org.evoionosp.noveliq.data.library.remote.mapper.toContinueListeningEntity
import org.evoionosp.noveliq.data.library.remote.mapper.toEntity
import org.evoionosp.noveliq.data.library.remote.mapper.toTrackEntities
import org.evoionosp.noveliq.data.library.remote.dto.UpdateProgressRequestDto
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.model.SyncStatus
import retrofit2.HttpException

@Singleton
class AudiobookRepositoryImpl @Inject constructor(
    private val database: NoveliqDatabase,
    private val audiobookDao: AudiobookDao,
    private val audiobookDetailDao: AudiobookDetailDao,
    private val continueListeningDao: ContinueListeningDao,
    private val syncStateDao: LibrarySyncStateDao,
    private val serviceFactory: AudiobookshelfLibraryServiceFactory,
    private val connectivityObserver: ConnectivityObserver,
    @param:Named("io") private val ioDispatcher: CoroutineDispatcher
) : AudiobookRepository {
    companion object {
        private const val TAG = "AudiobookRefresh"
    }

    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> {
        return audiobookDao.observeAudiobooks(libraryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAudiobook(libraryId: String, audiobookId: String): Flow<Audiobook?> {
        return audiobookDao.observeAudiobook(
            libraryId = libraryId,
            audiobookId = audiobookId
        ).map { entity ->
            entity?.toDomain()
        }
    }

    override fun observeAudiobookDetail(
        libraryId: String,
        audiobookId: String
    ): Flow<AudiobookDetail?> {
        return combine(
            audiobookDetailDao.observeDetail(libraryId, audiobookId),
            audiobookDetailDao.observeChapters(audiobookId),
            audiobookDetailDao.observeTracks(audiobookId)
        ) { detail, chapters, tracks ->
            detail?.toDomain(
                chapters = chapters,
                tracks = tracks
            )
        }
    }

    override fun observeContinueListening(libraryId: String): Flow<List<Audiobook>> {
        return continueListeningDao.observeContinueListening(libraryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> {
        return syncStateDao.observeSyncState(libraryId).map { it.toDomain() }
    }

    override suspend fun refreshAudiobookDetail(
        baseUrl: String,
        accessToken: String,
        libraryId: String,
        audiobookId: String
    ): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                return@withContext DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            }

            try {
                val item = serviceFactory.create(baseUrl).item(
                    authorization = "Bearer $accessToken",
                    itemId = audiobookId
                )
                if (item.id.isNullOrBlank()) {
                    DomainResult.Failure(CatalogError.NOT_FOUND)
                } else {
                    val now = System.currentTimeMillis()
                    val summary = item.toEntity(
                        baseUrl = baseUrl,
                        fallbackLibraryId = libraryId
                    )
                    val detail = item.toDetailEntity(
                        baseUrl = baseUrl,
                        fallbackLibraryId = libraryId,
                        refreshedAtMillis = now
                    )
                    if (summary == null || detail == null) {
                        return@withContext DomainResult.Failure(CatalogError.NOT_FOUND)
                    }
                    val chapters = item.toChapterEntities(audiobookId = detail.audiobookId)
                    val tracks = item.toTrackEntities(
                        baseUrl = baseUrl,
                        audiobookId = detail.audiobookId
                    )
                    database.withTransaction {
                        audiobookDao.upsertAudiobooks(listOf(summary))
                        audiobookDetailDao.upsertDetail(detail)
                        audiobookDetailDao.deleteChapters(detail.audiobookId)
                        audiobookDetailDao.deleteTracks(detail.audiobookId)
                        if (chapters.isNotEmpty()) {
                            audiobookDetailDao.upsertChapters(chapters)
                        }
                        if (tracks.isNotEmpty()) {
                            audiobookDetailDao.upsertTracks(tracks)
                        }
                    }
                    DomainResult.Success(Unit)
                }
            } catch (exception: HttpException) {
                val error = when (exception.code()) {
                    401, 403 -> CatalogError.AUTH
                    404 -> CatalogError.NOT_FOUND
                    else -> CatalogError.UNKNOWN
                }
                DomainResult.Failure(error)
            } catch (exception: IllegalArgumentException) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            }
        }
    }

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                markSyncFailure(libraryId, CatalogError.CONNECTIVITY_UNAVAILABLE)
                return@withContext DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            }

            syncStateDao.upsert(
                LibrarySyncStateEntity(
                    libraryId = libraryId,
                    status = "SYNCING",
                    lastSyncedAtMillis = currentLastSyncedAt(libraryId),
                    error = null
                )
            )

            try {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "Refreshing audiobooks for libraryId=$libraryId baseUrl=$baseUrl tokenPresent=${accessToken.isNotBlank()} tokenLength=${accessToken.length}"
                    )
                }
                val response = serviceFactory.create(baseUrl).libraryItems(
                    authorization = "Bearer $accessToken",
                    libraryId = libraryId
                )
                val audiobooks = response.results.orEmpty().mapNotNull { item ->
                    item.toEntity(
                        baseUrl = baseUrl,
                        fallbackLibraryId = libraryId
                    )
                }

                database.withTransaction {
                    audiobookDao.deleteByLibraryId(libraryId)
                    if (audiobooks.isNotEmpty()) {
                        audiobookDao.upsertAudiobooks(audiobooks)
                    }
                    syncStateDao.upsert(
                        LibrarySyncStateEntity(
                            libraryId = libraryId,
                            status = "SUCCESS",
                            lastSyncedAtMillis = System.currentTimeMillis(),
                            error = null
                        )
                    )
                }
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "Audiobook refresh success libraryId=$libraryId itemCount=${audiobooks.size}"
                    )
                }
                DomainResult.Success(Unit)
            } catch (exception: HttpException) {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.w(
                        TAG,
                        "Audiobook refresh failed with HTTP ${exception.code()} for GET /api/libraries/$libraryId/items at baseUrl=$baseUrl",
                        exception
                    )
                }
                val error = if (exception.code() == 401 || exception.code() == 403) {
                    CatalogError.AUTH
                } else {
                    CatalogError.UNKNOWN
                }
                markSyncFailure(libraryId, error)
                DomainResult.Failure(error)
            } catch (exception: IllegalArgumentException) {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.e(TAG, "Audiobook refresh invalid base URL for libraryId=$libraryId baseUrl=$baseUrl", exception)
                }
                markSyncFailure(libraryId, CatalogError.UNKNOWN)
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.w(TAG, "Audiobook refresh network failure for libraryId=$libraryId baseUrl=$baseUrl", exception)
                }
                markSyncFailure(libraryId, CatalogError.NETWORK)
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.e(TAG, "Audiobook refresh unexpected failure for libraryId=$libraryId baseUrl=$baseUrl", exception)
                }
                markSyncFailure(libraryId, CatalogError.UNKNOWN)
                DomainResult.Failure(CatalogError.UNKNOWN)
            }
        }
    }

    override suspend fun refreshContinueListening(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                return@withContext DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            }

            try {
                val response = serviceFactory.create(baseUrl).itemsInProgress(
                    authorization = "Bearer $accessToken",
                    limit = 50
                )
                val entities = response.results.orEmpty().filter { item ->
                    item.libraryId == libraryId
                }
                val audiobooks = entities.mapNotNull { item ->
                    item.toEntity(
                        baseUrl = baseUrl,
                        fallbackLibraryId = libraryId
                    )
                }
                val continueItems = entities.mapNotNull { item ->
                    item.toContinueListeningEntity(fallbackLibraryId = libraryId)
                }

                database.withTransaction {
                    if (audiobooks.isNotEmpty()) {
                        audiobookDao.upsertAudiobooks(audiobooks)
                    }
                    continueListeningDao.deleteByLibraryId(libraryId)
                    if (continueItems.isNotEmpty()) {
                        continueListeningDao.upsert(continueItems)
                    }
                }

                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "Continue Listening refresh success libraryId=$libraryId itemCount=${continueItems.size}"
                    )
                }
                DomainResult.Success(Unit)
            } catch (exception: HttpException) {
                if (org.evoionosp.noveliq.data.BuildConfig.DEBUG) {
                    Log.w(
                        TAG,
                        "Continue Listening refresh failed with HTTP ${exception.code()} for GET /api/me/items-in-progress at baseUrl=$baseUrl",
                        exception
                    )
                }
                val error = if (exception.code() == 401 || exception.code() == 403) {
                    CatalogError.AUTH
                } else if (exception.code() == 404) {
                    CatalogError.NOT_FOUND
                } else {
                    CatalogError.UNKNOWN
                }
                DomainResult.Failure(error)
            } catch (exception: IllegalArgumentException) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            }
        }
    }

    override suspend fun fetchProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): DomainResult<PlaybackProgress?> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                return@withContext DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            }

            try {
                val dto = serviceFactory.create(baseUrl).mediaProgress(
                    authorization = "Bearer $accessToken",
                    itemId = audiobookId
                )
                DomainResult.Success(
                    PlaybackProgress(
                        currentTimeSeconds = dto.currentTime ?: 0.0,
                        durationSeconds = dto.duration,
                        isFinished = dto.isFinished ?: false
                    )
                )
            } catch (exception: HttpException) {
                when (exception.code()) {
                    // No progress recorded for this item yet.
                    404 -> DomainResult.Success(null)
                    401, 403 -> DomainResult.Failure(CatalogError.AUTH)
                    else -> DomainResult.Failure(CatalogError.UNKNOWN)
                }
            } catch (exception: IllegalArgumentException) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            }
        }
    }

    override suspend fun saveProgress(
        baseUrl: String,
        accessToken: String,
        audiobookId: String,
        progress: PlaybackProgress
    ): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                return@withContext DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
            }

            val duration = progress.durationSeconds?.takeIf { it > 0 }
            val ratio = if (duration != null) {
                (progress.currentTimeSeconds / duration).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            try {
                val response = serviceFactory.create(baseUrl).updateMediaProgress(
                    authorization = "Bearer $accessToken",
                    itemId = audiobookId,
                    body = UpdateProgressRequestDto(
                        currentTime = progress.currentTimeSeconds,
                        duration = duration,
                        progress = ratio,
                        isFinished = progress.isFinished
                    )
                )
                if (response.isSuccessful) {
                    DomainResult.Success(Unit)
                } else {
                    val error = when (response.code()) {
                        401, 403 -> CatalogError.AUTH
                        404 -> CatalogError.NOT_FOUND
                        else -> CatalogError.UNKNOWN
                    }
                    DomainResult.Failure(error)
                }
            } catch (exception: HttpException) {
                val error = when (exception.code()) {
                    401, 403 -> CatalogError.AUTH
                    404 -> CatalogError.NOT_FOUND
                    else -> CatalogError.UNKNOWN
                }
                DomainResult.Failure(error)
            } catch (exception: IllegalArgumentException) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DomainResult.Failure(CatalogError.UNKNOWN)
            }
        }
    }

    private suspend fun markSyncFailure(
        libraryId: String,
        error: CatalogError
    ) {
        val hasCachedItems = audiobookDao.countByLibraryId(libraryId) > 0
        syncStateDao.upsert(
            LibrarySyncStateEntity(
                libraryId = libraryId,
                status = if (hasCachedItems) "STALE" else "FAILED",
                lastSyncedAtMillis = currentLastSyncedAt(libraryId),
                error = error.name
            )
        )
    }

    private suspend fun currentLastSyncedAt(libraryId: String): Long? {
        return syncStateDao.getSyncState(libraryId)?.lastSyncedAtMillis
    }
}
