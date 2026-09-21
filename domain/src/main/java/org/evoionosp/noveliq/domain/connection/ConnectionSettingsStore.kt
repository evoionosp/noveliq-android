package org.evoionosp.noveliq.domain.connection

import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader

interface ConnectionSettingsStore {
    val settings: Flow<ConnectionSettings>

    suspend fun setCustomHeaders(headers: List<ServerRequestHeader>)

    suspend fun setBypassSsl(enabled: Boolean)

    suspend fun setClientCertificateAlias(alias: String?)

    suspend fun setUserAgent(userAgent: String)

    suspend fun resetUserAgent()
}
