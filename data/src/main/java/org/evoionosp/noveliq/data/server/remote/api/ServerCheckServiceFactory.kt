package org.evoionosp.noveliq.data.server.remote.api

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.data.network.ManagedOkHttpClients
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Singleton
class ServerCheckServiceFactory
    @Inject
    constructor(
        private val httpClients: ManagedOkHttpClients,
    ) {
        private val serviceCache = mutableMapOf<String, ServerCheckApiService>()

        init {
            httpClients.registerInvalidator {
                synchronized(this) {
                    serviceCache.clear()
                }
            }
        }

        @Synchronized
        fun create(baseUrl: String): ServerCheckApiService {
            val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
            return serviceCache.getOrPut(normalizedBaseUrl) {
                Retrofit
                    .Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(httpClients.authClient)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ServerCheckApiService::class.java)
            }
        }
    }
