package org.evoionosp.noveliq.data.server.repository

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.server.remote.api.ServerServiceFactory
import org.evoionosp.noveliq.data.server.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.server.ServerRepository
import org.evoionosp.noveliq.domain.server.model.ServerResult
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import retrofit2.HttpException

class ServerRepositoryImpl(
    private val serviceFactory: ServerServiceFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
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
        return withContext(dispatcher) {
            try {
                val service = serviceFactory.create(baseUrl)
                val response = service.healthCheck()
                if (!response.isSuccessful) {
                    return@withContext ServerResult.Failure(
                        message = "Healthcheck failed.",
                        code = response.code()
                    )
                }
                val body = response.body()?.trim().orEmpty()
                if (body.equals("OK", ignoreCase = true)) {
                    ServerResult.Success(true)
                } else {
                    ServerResult.Failure("Healthcheck returned unexpected response.")
                }
            } catch (exception: HttpException) {
                ServerResult.Failure(
                    message = exception.message(),
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerResult.Failure(message = exception.message ?: "Invalid base URL.")
            } catch (exception: IOException) {
                ServerResult.Failure(message = "Network error. Please check your connection.")
            } catch (exception: Exception) {
                ServerResult.Failure(message = exception.message ?: "Unexpected error.")
            }
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ServerResult<T> {
        return withContext(dispatcher) {
            try {
                ServerResult.Success(block())
            } catch (exception: HttpException) {
                ServerResult.Failure(
                    message = exception.message(),
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                ServerResult.Failure(message = exception.message ?: "Invalid base URL.")
            } catch (exception: IOException) {
                ServerResult.Failure(message = "Network error. Please check your connection.")
            } catch (exception: Exception) {
                ServerResult.Failure(message = exception.message ?: "Unexpected error.")
            }
        }
    }
}
