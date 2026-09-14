package org.evoionosp.noveliq.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackAuthHeadersTest {
    @Test
    fun `formats the bearer authorization header`() {
        assertEquals(
            mapOf("Authorization" to "Bearer access-1"),
            authorizationHeaders("access-1"),
        )
    }

    @Test
    fun `formats an empty token without dropping the header`() {
        // The header must stay present (with an empty token) rather than disappear:
        // the data source layer always attaches it, and the server decides.
        assertEquals(
            mapOf("Authorization" to "Bearer "),
            authorizationHeaders(""),
        )
    }

    @Test
    fun `resolves the current access token from the session`() =
        runTest {
            val store = FakeSessionStore(session())

            assertEquals("access-1", store.currentAccessToken())
        }

    @Test
    fun `resolves an empty token when signed out`() =
        runTest {
            assertEquals("", FakeSessionStore(null).currentAccessToken())
        }

    private fun session() =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )
}

private class FakeSessionStore(
    initial: LoginSession?,
) : SessionStore {
    private val backing = MutableStateFlow(initial)

    override val session = backing
    override val lastServerUrl = MutableStateFlow(initial?.baseUrl)

    override suspend fun saveSession(session: LoginSession): LoginSession {
        backing.value = session
        return session
    }

    override suspend fun clearSession() {
        backing.value = null
    }
}
