package org.evoionosp.noveliq.data.audiobook.repository

import androidx.room.withTransaction
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.audiobook.local.dao.AudiobookDao
import org.evoionosp.noveliq.data.audiobook.local.mapper.toDomain
import org.evoionosp.noveliq.data.connectivity.ConnectivityObserver
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.entity.LibrarySyncStateEntity
import org.evoionosp.noveliq.data.library.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.library.remote.mapper.toEntity
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.repository.AudiobookRepository
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
    private val connectivityObserver: ConnectivityObserver
) : AudiobookRepository {
    override fun observeAudiobooks(libraryId: String): Flow<List<Audiobook>> {
        return audiobookDao.observeAudiobooks(libraryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLibrarySyncStatus(libraryId: String): Flow<SyncStatus> {
        return syncStateDao.observeSyncState(libraryId).map { it.toDomain() }
    }

    override suspend fun refreshAudiobooks(
        baseUrl: String,
        accessToken: String,
        libraryId: String
    ): DomainResult<Unit> {
        return withContext(Dispatchers.IO) {
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
                DomainResult.Success(Unit)
            } catch (exception: HttpException) {
                val error = if (exception.code() == 401 || exception.code() == 403) {
                    CatalogError.AUTH
                } else {
                    CatalogError.UNKNOWN
                }
                markSyncFailure(libraryId, error)
                DomainResult.Failure(error)
            } catch (exception: IllegalArgumentException) {
                markSyncFailure(libraryId, CatalogError.UNKNOWN)
                DomainResult.Failure(CatalogError.UNKNOWN)
            } catch (exception: IOException) {
                markSyncFailure(libraryId, CatalogError.NETWORK)
                DomainResult.Failure(CatalogError.NETWORK)
            } catch (exception: Exception) {
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
