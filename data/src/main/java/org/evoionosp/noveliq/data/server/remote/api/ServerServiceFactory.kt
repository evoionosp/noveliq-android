package org.evoionosp.noveliq.data.server.remote.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val serviceCache = mutableMapOf<String, ServerApiService>()

    @Synchronized
    fun create(baseUrl: String): ServerApiService {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        return serviceCache.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ServerApiService::class.java)
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        require(trimmed.isNotEmpty()) { "Base URL must not be blank." }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
