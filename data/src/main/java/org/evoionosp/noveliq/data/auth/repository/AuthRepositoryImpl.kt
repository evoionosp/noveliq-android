package org.evoionosp.noveliq.data.auth.repository

import android.util.Log
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.evoionosp.noveliq.data.auth.remote.api.LoginServiceFactory
import org.evoionosp.noveliq.data.auth.remote.dto.LoginRequestDto
import org.evoionosp.noveliq.data.auth.remote.mapper.toDomain
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import retrofit2.HttpException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val serviceFactory: LoginServiceFactory,
    @param:Named("io") private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {

        Log.d("AuthRepositoryImpl", "login: $baseUrl")

        return withContext(ioDispatcher) {
            try {
                val service = serviceFactory.create(baseUrl)
                val response = service.login(
                    request = LoginRequestDto(
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
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                LoginResult.Failure(error = AuthError.UNEXPECTED)
            }
        }
    }

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String
    ): LoginResult {
        return withContext(ioDispatcher) {
            try {
                val response = serviceFactory.create(baseUrl).refreshToken(
                    refreshToken = refreshToken
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
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                LoginResult.Failure(error = AuthError.UNEXPECTED)
            }
        }
    }
}
