package org.evoionosp.noveliq.data.auth.remote.mapper

import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import org.evoionosp.noveliq.domain.auth.model.LoginData

internal fun LoginResponseDto.toDomain(): LoginData {
    return LoginData(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId
    )
}
