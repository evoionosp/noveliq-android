package org.evoionosp.noveliq.data.server.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.server.remote.api.ServerServiceFactory
import org.evoionosp.noveliq.data.server.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.server.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serviceFactory: ServerServiceFactory
) : ServerRepository {

    override suspend fun ping(baseUrl: String): ServerResult<Boolean> {
        return safeCall {
            val service = serviceFactory.create(baseUrl)
            service.ping().success
        }
    }

    override suspend fun getStatus(baseUrl: String): ServerResult<ServerStatus> {
        return safeCall {
            val service = serviceFactory.create(baseUrl)
            service.status().toDomain()
        }
    }

    override suspend fun healthCheck(baseUrl: String): ServerResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val service = serviceFactory.create(baseUrl)
                val response = service.healthCheck()
                if (!response.isSuccessful) {
                    return@withContext ServerResult.Failure(
                        error = ServerError.HEALTHCHECK_FAILED,
                        code = response.code()
                    )
                }
                val body = response.body()?.trim().orEmpty()
                if (body.equals("OK", ignoreCase = true)) {
                    ServerResult.Success(true)
                } else {
                    ServerResult.Failure(ServerError.HEALTHCHECK_UNEXPECTED)
                }
            } catch (exception: HttpException) {
                ServerResult.Failure(
                    error = ServerError.HTTP,
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerResult.Failure(ServerError.INVALID_BASE_URL)
            } catch (exception: IOException) {
                ServerResult.Failure(ServerError.NETWORK)
            } catch (exception: Exception) {
                ServerResult.Failure(ServerError.UNKNOWN)
            }
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ServerResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                ServerResult.Success(block())
            } catch (exception: HttpException) {
                ServerResult.Failure(
                    error = ServerError.HTTP,
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerResult.Failure(ServerError.INVALID_BASE_URL)
            } catch (exception: IOException) {
                ServerResult.Failure(ServerError.NETWORK)
            } catch (exception: Exception) {
                ServerResult.Failure(ServerError.UNKNOWN)
            }
        }
    }
}
