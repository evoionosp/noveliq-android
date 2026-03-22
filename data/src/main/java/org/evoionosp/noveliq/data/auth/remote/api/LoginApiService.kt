package org.evoionosp.noveliq.data.auth.remote.api

import org.evoionosp.noveliq.data.auth.remote.dto.LoginRequestDto
import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApiService {
    @POST(LOGIN_PATH)
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    companion object {
        const val LOGIN_PATH = "login"
    }
}
