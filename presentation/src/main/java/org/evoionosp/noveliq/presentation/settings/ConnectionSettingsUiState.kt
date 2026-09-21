package org.evoionosp.noveliq.presentation.settings

import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader

data class ConnectionSettingsUiState(
    val headers: List<ServerRequestHeader> = emptyList(),
    val bypassSsl: Boolean = false,
    val clientCertificateAlias: String? = null,
    val userAgent: String = "",
)
