package org.evoionosp.noveliq.data.server.remote.api

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.di.NetworkModule
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Singleton
class ServerCheckServiceFactory
    @Inject
    constructor(
        @param:Named(NetworkModule.AUTH_CLIENT) private val okHttpClient: OkHttpClient,
    ) {
        private val serviceCache = mutableMapOf<String, ServerCheckApiService>()

        @Synchronized
        fun create(baseUrl: String): ServerCheckApiService {
            val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
            return serviceCache.getOrPut(normalizedBaseUrl) {
                Retrofit
                    .Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ServerCheckApiService::class.java)
            }
        }
    }
