package org.evoionosp.noveliq.data.auth

import org.evoionosp.noveliq.data.auth.remote.api.LoginServiceFactory
import org.evoionosp.noveliq.data.network.OkHttpProvider
import org.evoionosp.noveliq.data.auth.repository.AuthRepositoryImpl
import org.evoionosp.noveliq.domain.auth.AuthRepository

object AuthRepositoryProvider {
    fun create(): AuthRepository {
        val okHttpClient = OkHttpProvider.create()
        val serviceFactory = LoginServiceFactory(okHttpClient)
        return AuthRepositoryImpl(serviceFactory)
    }
}
