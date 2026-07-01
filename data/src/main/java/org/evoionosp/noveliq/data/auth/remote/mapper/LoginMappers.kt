package org.evoionosp.noveliq.data.auth.remote.mapper

import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import org.evoionosp.noveliq.domain.auth.model.LoginData

internal fun LoginResponseDto.toDomain(): LoginData {
    val resolvedAccessToken = accessToken ?: user?.token
    val resolvedRefreshToken = refreshToken ?: user?.refreshToken
    val resolvedUserId = userId ?: user?.id
    return LoginData(
        accessToken = resolvedAccessToken,
        refreshToken = resolvedRefreshToken,
        userId = resolvedUserId
    )
}
