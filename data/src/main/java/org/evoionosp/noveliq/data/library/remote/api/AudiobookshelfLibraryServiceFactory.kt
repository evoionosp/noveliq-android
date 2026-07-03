package org.evoionosp.noveliq.data.library.remote.api

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.network.UrlUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class AudiobookshelfLibraryServiceFactory @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val serviceCache = mutableMapOf<String, AudiobookshelfLibraryApiService>()

    @Synchronized
    fun create(baseUrl: String): AudiobookshelfLibraryApiService {
        val normalizedBaseUrl = UrlUtils.normalizeBaseUrl(baseUrl)
        return serviceCache.getOrPut(normalizedBaseUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AudiobookshelfLibraryApiService::class.java)
        }
    }

}
