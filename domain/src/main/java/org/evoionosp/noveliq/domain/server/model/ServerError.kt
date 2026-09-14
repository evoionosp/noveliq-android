package org.evoionosp.noveliq.domain.server.model

enum class ServerError {
    INVALID_BASE_URL,
    NETWORK,
    SERVER_NOT_FOUND,
    HTTP,
    PING_FAILED,
    HEALTHCHECK_FAILED,
    HEALTHCHECK_UNEXPECTED,
    UNKNOWN,
}
