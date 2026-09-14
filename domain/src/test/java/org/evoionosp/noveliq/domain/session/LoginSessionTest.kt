package org.evoionosp.noveliq.domain.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginSessionTest {
    private val nowEpochSeconds = 1_700_000_000L

    @Test
    fun `isValid requires a non-blank token username and base url`() {
        assertTrue(session().isValid())
        assertFalse(session().copy(accessToken = "  ").isValid())
        assertFalse(session().copy(username = "").isValid())
        assertFalse(session().copy(baseUrl = "").isValid())
    }

    @Test
    fun `isValid ignores expiry so an expired session stays recoverable`() {
        assertTrue(session(expiresAt = nowEpochSeconds - 3_600).isValid())
    }

    @Test
    fun `isAccessTokenExpired is false when the expiry is unknown`() {
        assertFalse(session(expiresAt = null).isAccessTokenExpired(nowEpochSeconds))
    }

    @Test
    fun `isAccessTokenExpired is true once the token has lapsed`() {
        assertTrue(session(expiresAt = nowEpochSeconds - 1).isAccessTokenExpired(nowEpochSeconds))
    }

    @Test
    fun `isAccessTokenExpired treats the skew window as already expired`() {
        // 30s of life left is inside the default 60s skew: a request started now
        // could still be in flight when the token dies, so refresh up front.
        assertTrue(session(expiresAt = nowEpochSeconds + 30).isAccessTokenExpired(nowEpochSeconds))
        assertFalse(session(expiresAt = nowEpochSeconds + 61).isAccessTokenExpired(nowEpochSeconds))
    }

    @Test
    fun `isAccessTokenExpired honours a custom skew`() {
        val expiresAt = nowEpochSeconds + 30
        assertFalse(session(expiresAt = expiresAt).isAccessTokenExpired(nowEpochSeconds, skewSeconds = 10))
        assertTrue(session(expiresAt = expiresAt).isAccessTokenExpired(nowEpochSeconds, skewSeconds = 60))
    }

    private fun session(expiresAt: Long? = nowEpochSeconds + 3_600) =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
            accessTokenExpiresAtEpochSeconds = expiresAt,
        )
}
