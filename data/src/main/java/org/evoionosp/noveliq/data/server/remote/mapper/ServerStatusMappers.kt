package org.evoionosp.noveliq.data.server.remote.mapper

import org.evoionosp.noveliq.data.server.remote.dto.LoginStatusResponseDto
import org.evoionosp.noveliq.domain.server.model.ServerStatus

internal fun LoginStatusResponseDto.toDomain(): ServerStatus {
    return ServerStatus(
        app = app.orEmpty(),
        serverVersion = serverVersion.orEmpty(),
        isInit = isInit ?: false,
        language = language.orEmpty(),
        authMethods = authMethods ?: emptyList(),
        authLoginCustomMessage = authFormData?.authLoginCustomMessage.orEmpty()
    )
}
