package org.evoionosp.noveliq.data.auth.repository

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.auth.remote.api.LoginServiceFactory
import org.evoionosp.noveliq.data.auth.remote.dto.LoginRequestDto
import org.evoionosp.noveliq.data.auth.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.auth.AuthRepository
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val serviceFactory: LoginServiceFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {
        return withContext(dispatcher) {
            try {
                val service = serviceFactory.create(baseUrl)
                val response = service.login(
                    LoginRequestDto(
                        username = username,
                        password = password
                    )
                )
                LoginResult.Success(response.toDomain())
            } catch (exception: HttpException) {
                LoginResult.Failure(
                    message = exception.message(),
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                LoginResult.Failure(message = exception.message ?: "Invalid base URL.")
            } catch (exception: IOException) {
                LoginResult.Failure(message = "Network error. Please check your connection.")
            } catch (exception: Exception) {
                LoginResult.Failure(message = exception.message ?: "Unexpected error.")
            }
        }
    }
}
