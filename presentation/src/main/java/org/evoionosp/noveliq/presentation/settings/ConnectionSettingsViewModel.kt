package org.evoionosp.noveliq.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader

@HiltViewModel
class ConnectionSettingsViewModel
    @Inject
    constructor(
        private val store: ConnectionSettingsStore,
    ) : ViewModel() {
        val uiState: StateFlow<ConnectionSettingsUiState> =
            store.settings
                .map { settings ->
                    ConnectionSettingsUiState(
                        headers = settings.customHeaders,
                        bypassSsl = settings.bypassSsl,
                        clientCertificateAlias = settings.clientCertificateAlias,
                        userAgent = settings.userAgent,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ConnectionSettingsUiState(),
                )

        /** Adds a new header or replaces the one with the same id. */
        fun saveHeader(header: ServerRequestHeader) {
            viewModelScope.launch {
                val current = store.settings.first().customHeaders
                val updated =
                    if (current.any { it.id == header.id }) {
                        current.map { if (it.id == header.id) header else it }
                    } else {
                        current + header
                    }
                store.setCustomHeaders(updated)
            }
        }

        fun removeHeader(id: String) {
            viewModelScope.launch {
                val updated =
                    store.settings
                        .first()
                        .customHeaders
                        .filterNot { it.id == id }
                store.setCustomHeaders(updated)
            }
        }

        fun setBypassSsl(enabled: Boolean) {
            viewModelScope.launch {
                store.setBypassSsl(enabled)
            }
        }

        fun setClientCertificateAlias(alias: String?) {
            viewModelScope.launch {
                store.setClientCertificateAlias(alias)
            }
        }

        fun setUserAgent(userAgent: String) {
            viewModelScope.launch {
                store.setUserAgent(userAgent)
            }
        }

        fun resetUserAgent() {
            viewModelScope.launch {
                store.resetUserAgent()
            }
        }
    }
