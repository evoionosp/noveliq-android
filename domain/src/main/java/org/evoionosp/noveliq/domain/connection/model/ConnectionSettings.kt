package org.evoionosp.noveliq.domain.connection.model

/**
 * Device-level server connectivity: the knobs non-standard hosts need
 * (reverse proxies, self-signed TLS, mTLS gateways, UA-filtering WAFs).
 * Unlike the login session these survive logout — they describe how to
 * reach the server, so they must be in place before a login can succeed.
 */
data class ConnectionSettings(
    val customHeaders: List<ServerRequestHeader> = emptyList(),
    val bypassSsl: Boolean = false,
    val clientCertificateAlias: String? = null,
    val userAgent: String,
)
