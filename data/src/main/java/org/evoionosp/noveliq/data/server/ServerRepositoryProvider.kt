package org.evoionosp.noveliq.data.server

import org.evoionosp.noveliq.data.network.OkHttpProvider
import org.evoionosp.noveliq.data.server.remote.api.ServerServiceFactory
import org.evoionosp.noveliq.data.server.repository.ServerRepositoryImpl
import org.evoionosp.noveliq.domain.server.ServerRepository

object ServerRepositoryProvider {
    fun create(): ServerRepository {
        val okHttpClient = OkHttpProvider.create()
        val serviceFactory = ServerServiceFactory(okHttpClient)
        return ServerRepositoryImpl(serviceFactory)
    }
}
