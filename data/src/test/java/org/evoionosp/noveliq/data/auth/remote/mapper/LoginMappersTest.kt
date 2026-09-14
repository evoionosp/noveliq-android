package org.evoionosp.noveliq.data.auth.remote.mapper

import org.evoionosp.noveliq.data.auth.remote.dto.LoginResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginMappersTest {
    @Test
    fun `toDomain prefers top-level tokens over nested user`() {
        val data =
            LoginResponseDto(
                accessToken = "top-access",
                refreshToken = "top-refresh",
                userId = "top-id",
                user =
                    LoginResponseDto.UserDto(
                        token = "nested-access",
                        refreshToken = "nested-refresh",
                        id = "nested-id",
                    ),
            ).toDomain()

        assertEquals("top-access", data.accessToken)
        assertEquals("top-refresh", data.refreshToken)
        assertEquals("top-id", data.userId)
    }

    @Test
    fun `toDomain falls back to nested user when top-level is absent`() {
        val data =
            LoginResponseDto(
                user =
                    LoginResponseDto.UserDto(
                        token = "nested-access",
                        refreshToken = "nested-refresh",
                        id = "nested-id",
                    ),
            ).toDomain()

        assertEquals("nested-access", data.accessToken)
        assertEquals("nested-refresh", data.refreshToken)
        assertEquals("nested-id", data.userId)
    }

    @Test
    fun `toDomain allows all-null login data`() {
        val data = LoginResponseDto().toDomain()

        assertNull(data.accessToken)
        assertNull(data.refreshToken)
        assertNull(data.userId)
    }
}
