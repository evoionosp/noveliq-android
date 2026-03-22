package org.evoionosp.noveliq.data.server.remote.api

import org.evoionosp.noveliq.data.server.remote.dto.PingResponseDto
import org.evoionosp.noveliq.data.server.remote.dto.StatusResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface ServerApiService {
    @GET("ping")
    suspend fun ping(): PingResponseDto

    @GET("status")
    suspend fun status(): StatusResponseDto

    @GET("healthcheck")
    suspend fun healthCheck(): Response<String>
}
