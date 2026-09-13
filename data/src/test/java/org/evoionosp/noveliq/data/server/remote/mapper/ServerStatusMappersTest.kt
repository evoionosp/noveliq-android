package org.evoionosp.noveliq.data.server.remote.mapper

import org.evoionosp.noveliq.data.server.remote.dto.AuthFormDataDto
import org.evoionosp.noveliq.data.server.remote.dto.LoginStatusResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerStatusMappersTest {
    @Test
    fun `toDomain maps all status fields`() {
        val status =
            LoginStatusResponseDto(
                app = "audiobookshelf",
                serverVersion = "2.0.0",
                isInit = true,
                language = "en",
                authMethods = listOf("local", "ldap"),
                authFormData = AuthFormDataDto(authLoginCustomMessage = "Welcome"),
            ).toDomain()

        assertEquals("audiobookshelf", status.app)
        assertEquals("2.0.0", status.serverVersion)
        assertTrue(status.isInit)
        assertEquals("en", status.language)
        assertEquals(listOf("local", "ldap"), status.authMethods)
        assertEquals("Welcome", status.authLoginCustomMessage)
    }

    @Test
    fun `toDomain defaults absent fields`() {
        val status = LoginStatusResponseDto().toDomain()

        assertEquals("", status.app)
        assertEquals("", status.serverVersion)
        assertFalse(status.isInit)
        assertEquals("", status.language)
        assertEquals(emptyList<String>(), status.authMethods)
        assertEquals("", status.authLoginCustomMessage)
    }
}
