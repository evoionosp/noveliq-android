package org.evoionosp.noveliq.data.auth.remote.api

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.data.network.ManagedOkHttpClients
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class LoginServiceFactory
    @Inject
    constructor(
        private val httpClients: ManagedOkHttpClients,
    ) {
        private val serviceCache = mutableMapOf<String, LoginApiService>()

        init {
            httpClients.registerInvalidator {
                synchronized(this) {
                    serviceCache.clear()
                }
            }
        }

        @Synchronized
        fun create(baseUrl: String): LoginApiService {
            val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
            return serviceCache.getOrPut(normalizedBaseUrl) {
                Retrofit
                    .Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(httpClients.authClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(LoginApiService::class.java)
            }
        }
    }
