package org.evoionosp.noveliq.data.auth.remote.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val serviceCache = mutableMapOf<String, LoginApiService>()

    @Synchronized
    fun create(baseUrl: String): LoginApiService {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        return serviceCache.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LoginApiService::class.java)
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        require(trimmed.isNotEmpty()) { "Base URL must not be blank." }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
