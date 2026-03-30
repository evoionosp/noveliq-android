package org.evoionosp.noveliq.data.server.remote.api

import org.evoionosp.noveliq.data.server.remote.dto.ServerPingResponseDto
import org.evoionosp.noveliq.data.server.remote.dto.LoginStatusResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface ServerCheckApiService {
    @GET("ping")
    suspend fun ping(): ServerPingResponseDto

    @GET("status")
    suspend fun status(): LoginStatusResponseDto

    @GET("healthcheck")
    suspend fun healthCheck(): Response<String>
}
