package org.evoionosp.noveliq.data.auth.remote.mapper

import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import org.evoionosp.noveliq.domain.auth.model.LoginData

internal fun LoginResponseDto.toDomain(): LoginData {
    val resolvedAccessToken = accessToken ?: user?.token
    val resolvedUserId = userId ?: user?.id
    return LoginData(
        accessToken = resolvedAccessToken,
        refreshToken = refreshToken,
        userId = resolvedUserId
    )
}
