package org.evoionosp.noveliq.domain.server.model

data class ServerStatus(
    val app: String,
    val serverVersion: String,
    val isInit: Boolean,
    val language: String,
    val authMethods: List<String>,
    val authLoginCustomMessage: String
)
