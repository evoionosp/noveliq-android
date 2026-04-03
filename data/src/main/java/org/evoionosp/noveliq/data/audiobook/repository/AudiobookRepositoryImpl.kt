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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.audiobook.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.data.library.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.library.remote.mapper.toChapterDomainList
import org.evoionosp.noveliq.data.library.remote.mapper.toEntity
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
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

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> {
        return syncStateDao.observeSyncState(libraryId).map { it.toDomain() }
    }

    override suspend fun getAudiobookChapters(
        baseUrl: String,
        accessToken: String,
        audiobookId: String
    ): DomainResult<List<AudiobookChapter>> {
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
                    DomainResult.Success(item.toChapterDomainList())
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
                        serviceFactory = serviceFactory,
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
