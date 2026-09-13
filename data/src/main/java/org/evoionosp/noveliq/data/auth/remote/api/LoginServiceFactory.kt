package org.evoionosp.noveliq.data.auth.remote.api

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.di.NetworkModule
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class LoginServiceFactory
    @Inject
    constructor(
        @param:Named(NetworkModule.AUTH_CLIENT) private val okHttpClient: OkHttpClient,
    ) {
        private val serviceCache = mutableMapOf<String, LoginApiService>()

        @Synchronized
        fun create(baseUrl: String): LoginApiService {
            val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
            return serviceCache.getOrPut(normalizedBaseUrl) {
                Retrofit
                    .Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(LoginApiService::class.java)
            }
        }
    }
