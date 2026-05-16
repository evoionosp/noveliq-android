package org.evoionosp.noveliq.data.auth.remote.api

import org.evoionosp.noveliq.data.auth.remote.dto.LoginRequestDto
import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LoginApiService {
    @POST(LOGIN_PATH)
    suspend fun login(
        @Header("x-return-tokens") returnTokens: String = "true",
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("x-refresh-token") refreshToken: String
    ): LoginResponseDto

    companion object {
        const val LOGIN_PATH = "login"
    }
}
