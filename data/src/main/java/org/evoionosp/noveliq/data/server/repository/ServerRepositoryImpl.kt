package org.evoionosp.noveliq.data.server.repository

import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.server.remote.api.ServerCheckServiceFactory
import org.evoionosp.noveliq.data.server.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.evoionosp.noveliq.domain.server.repository.ServerRepository
import retrofit2.HttpException

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serviceFactory: ServerCheckServiceFactory,
    @param:Named("io") private val ioDispatcher: CoroutineDispatcher
) : ServerRepository {

    override suspend fun ping(baseUrl: String): ServerCheckResult<Boolean> {
        return safeCall {
            val service = serviceFactory.create(baseUrl)
            service.ping().success
        }
    }

    override suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus> {
        return safeCall {
            val service = serviceFactory.create(baseUrl)
            service.status().toDomain()
        }
    }

    override suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean> {
        return withContext(ioDispatcher) {
            try {
                val service = serviceFactory.create(baseUrl)
                val response = service.healthCheck()
                if (!response.isSuccessful) {
                    return@withContext ServerCheckResult.Failure(
                        error = ServerError.HEALTHCHECK_FAILED,
                        code = response.code()
                    )
                }
                val body = response.body()?.trim().orEmpty()
                if (body.equals("OK", ignoreCase = true)) {
                    ServerCheckResult.Success(true)
                } else {
                    ServerCheckResult.Failure(ServerError.HEALTHCHECK_UNEXPECTED)
                }
            } catch (exception: HttpException) {
                ServerCheckResult.Failure(
                    error = ServerError.HTTP,
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerCheckResult.Failure(ServerError.INVALID_BASE_URL)
            } catch (exception: IOException) {
                ServerCheckResult.Failure(ServerError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ServerCheckResult.Failure(ServerError.UNKNOWN)
            }
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ServerCheckResult<T> {
        return withContext(ioDispatcher) {
            try {
                ServerCheckResult.Success(block())
            } catch (exception: HttpException) {
                ServerCheckResult.Failure(
                    error = ServerError.HTTP,
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerCheckResult.Failure(ServerError.INVALID_BASE_URL)
            } catch (exception: IOException) {
                ServerCheckResult.Failure(ServerError.NETWORK)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ServerCheckResult.Failure(ServerError.UNKNOWN)
            }
        }
    }
}
