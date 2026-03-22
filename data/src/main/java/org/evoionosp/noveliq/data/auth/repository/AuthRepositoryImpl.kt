package org.evoionosp.noveliq.data.auth.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.auth.remote.api.LoginServiceFactory
import org.evoionosp.noveliq.data.auth.remote.dto.LoginRequestDto
import org.evoionosp.noveliq.data.auth.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val serviceFactory: LoginServiceFactory
) : AuthRepository {

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {
        return withContext(Dispatchers.IO) {
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
                    error = AuthError.HTTP,
                    code = exception.code()
                )
            } catch (exception: IllegalArgumentException) {
                LoginResult.Failure(error = AuthError.INVALID_BASE_URL)
            } catch (exception: IOException) {
                LoginResult.Failure(error = AuthError.NETWORK)
            } catch (exception: Exception) {
                LoginResult.Failure(error = AuthError.UNEXPECTED)
            }
        }
    }
}
