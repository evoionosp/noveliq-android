package org.evoionosp.noveliq.data.library.repository

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
import org.evoionosp.noveliq.data.library.local.dao.LibraryDao
import org.evoionosp.noveliq.data.library.local.dao.LibrarySyncStateDao
import org.evoionosp.noveliq.data.library.local.db.NoveliqDatabase
import org.evoionosp.noveliq.data.library.local.mapper.toDomain
import org.evoionosp.noveliq.data.library.remote.api.AudiobookshelfLibraryServiceFactory
import org.evoionosp.noveliq.data.library.remote.mapper.isAudiobookLibrary
import org.evoionosp.noveliq.data.library.remote.mapper.toEntity
import org.evoionosp.noveliq.domain.library.model.AudiobookLibrary
import org.evoionosp.noveliq.domain.library.model.CatalogError
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.library.repository.LibraryRepository
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver
import retrofit2.HttpException

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val database: NoveliqDatabase,
    private val libraryDao: LibraryDao,
    private val audiobookDao: AudiobookDao,
    private val syncStateDao: LibrarySyncStateDao,
    private val serviceFactory: AudiobookshelfLibraryServiceFactory,
    private val connectivityObserver: ConnectivityObserver,
    @param:Named("io") private val ioDispatcher: CoroutineDispatcher
) : LibraryRepository {
    override fun observeLibraries(): Flow<List<AudiobookLibrary>> {
        return libraryDao.observeLibraries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeSelectedLibrary(): Flow<AudiobookLibrary?> {
        return libraryDao.observeSelectedLibrary().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun refreshLibraries(
        baseUrl: String,
        accessToken: String
    ): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            if (!connectivityObserver.isConnected()) {
                val hasCachedLibraries = libraryDao.countLibraries() > 0
                if (hasCachedLibraries) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Failure(CatalogError.CONNECTIVITY_UNAVAILABLE)
                }
            } else {
                try {
                    val previousSelectionId = libraryDao.getSelectedLibrary()?.id
                    val libraries = serviceFactory.create(baseUrl)
                        .libraries(authorization = "Bearer $accessToken")
                        .libraries
                        .orEmpty()
                        .filter { it.isAudiobookLibrary() }

                    if (libraries.isEmpty()) {
                        database.withTransaction {
                            libraryDao.deleteAll()
                            audiobookDao.deleteAll()
                            syncStateDao.deleteAll()
                        }
                        return@withContext DomainResult.Failure(CatalogError.NO_AUDIOBOOK_LIBRARIES)
                    }

                    val selectedLibraryId = libraries.firstOrNull { it.id == previousSelectionId }?.id
                        ?: libraries.firstNotNullOfOrNull { dto -> dto.id }

                    val entities = libraries.mapNotNull { dto ->
                        dto.toEntity(isSelected = dto.id == selectedLibraryId)
                    }

                    database.withTransaction {
                        libraryDao.upsertLibraries(entities)
                        libraryDao.deleteLibrariesNotIn(entities.map { it.id })
                        audiobookDao.deleteByLibrariesNotIn(entities.map { it.id })
                        syncStateDao.deleteByLibrariesNotIn(entities.map { it.id })
                    }
                    DomainResult.Success(Unit)
                } catch (exception: HttpException) {
                    if (libraryDao.countLibraries() > 0) {
                        DomainResult.Success(Unit)
                    } else {
                        val error = if (exception.code() == 401 || exception.code() == 403) {
                            CatalogError.AUTH
                        } else {
                            CatalogError.UNKNOWN
                        }
                        DomainResult.Failure(error)
                    }
                } catch (exception: IllegalArgumentException) {
                    DomainResult.Failure(CatalogError.UNKNOWN)
                } catch (exception: IOException) {
                    if (libraryDao.countLibraries() > 0) {
                        DomainResult.Success(Unit)
                    } else {
                        DomainResult.Failure(CatalogError.NETWORK)
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    DomainResult.Failure(CatalogError.UNKNOWN)
                }
            }
        }
    }

    override suspend fun selectLibrary(libraryId: String): DomainResult<Unit> {
        return withContext(ioDispatcher) {
            val libraryExists = libraryDao.getLibraries().any { it.id == libraryId }
            if (!libraryExists) {
                return@withContext DomainResult.Failure(CatalogError.NOT_FOUND)
            }

            database.withTransaction {
                libraryDao.selectLibrary(libraryId)
            }
            DomainResult.Success(Unit)
        }
    }
}
