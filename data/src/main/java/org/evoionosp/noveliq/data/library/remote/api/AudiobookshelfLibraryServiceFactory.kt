package org.evoionosp.noveliq.data.library.remote.api

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.data.network.ManagedOkHttpClients
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class AudiobookshelfLibraryServiceFactory
    @Inject
    constructor(
        private val httpClients: ManagedOkHttpClients,
    ) {
        private val serviceCache = mutableMapOf<String, AudiobookshelfLibraryApiService>()

        init {
            httpClients.registerInvalidator {
                synchronized(this) {
                    serviceCache.clear()
                }
            }
        }

        @Synchronized
        fun create(baseUrl: String): AudiobookshelfLibraryApiService {
            val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
            return serviceCache.getOrPut(normalizedBaseUrl) {
                Retrofit
                    .Builder()
                    .baseUrl(normalizedBaseUrl)
                    .client(httpClients.apiClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AudiobookshelfLibraryApiService::class.java)
            }
        }
    }
