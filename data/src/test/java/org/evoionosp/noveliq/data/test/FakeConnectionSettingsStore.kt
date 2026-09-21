package org.evoionosp.noveliq.data.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader

class FakeConnectionSettingsStore(
    initial: ConnectionSettings = ConnectionSettings(userAgent = "FakeAgent/1.0"),
) : ConnectionSettingsStore {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<ConnectionSettings> = state.asStateFlow()

    override suspend fun setCustomHeaders(headers: List<ServerRequestHeader>) {
        state.update { it.copy(customHeaders = headers) }
    }

    override suspend fun setBypassSsl(enabled: Boolean) {
        state.update { it.copy(bypassSsl = enabled) }
    }

    override suspend fun setClientCertificateAlias(alias: String?) {
        state.update { it.copy(clientCertificateAlias = alias) }
    }

    override suspend fun setUserAgent(userAgent: String) {
        state.update { it.copy(userAgent = userAgent) }
    }

    override suspend fun resetUserAgent() {
        state.update { it.copy(userAgent = "FakeAgent/1.0") }
    }

    fun emit(settings: ConnectionSettings) {
        state.value = settings
    }
}
