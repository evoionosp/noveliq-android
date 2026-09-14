package org.evoionosp.noveliq.data.network

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.evoionosp.noveliq.domain.auth.SessionRefreshCoordinator
import org.evoionosp.noveliq.domain.auth.TokenRefreshResult
import org.evoionosp.noveliq.domain.session.LoginSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {
    private val coordinator = mockk<SessionRefreshCoordinator>()
    private val authenticator = TokenAuthenticator(coordinator)

    @Test
    fun `refresh success replays the request with the new token`() {
        coEvery { coordinator.refresh("stale") } returns
            TokenRefreshResult.Refreshed(session(accessToken = "fresh"))

        val replayed = authenticator.authenticate(null, response(requestToken = "stale"))

        assertEquals("Bearer fresh", replayed?.header("Authorization"))
    }

    @Test
    fun `logged out and retryable results give up`() {
        coEvery { coordinator.refresh(any()) } returns TokenRefreshResult.LoggedOut
        assertNull(authenticator.authenticate(null, response(requestToken = "stale")))

        coEvery { coordinator.refresh(any()) } returns TokenRefreshResult.Retryable
        assertNull(authenticator.authenticate(null, response(requestToken = "stale")))
    }

    @Test
    fun `unchanged or blank rotated tokens give up`() {
        coEvery { coordinator.refresh(any()) } returns
            TokenRefreshResult.Refreshed(session(accessToken = "stale"))
        assertNull(authenticator.authenticate(null, response(requestToken = "stale")))

        coEvery { coordinator.refresh(any()) } returns
            TokenRefreshResult.Refreshed(session(accessToken = "  "))
        assertNull(authenticator.authenticate(null, response(requestToken = "other")))
    }

    @Test
    fun `second attempt gives up without another refresh`() {
        val firstAttempt = response(requestToken = "stale")
        val secondAttempt = response(requestToken = "fresh", priorResponse = firstAttempt)

        coEvery { coordinator.refresh(any()) } returns
            TokenRefreshResult.Refreshed(session(accessToken = "fresh"))

        assertNull(authenticator.authenticate(null, secondAttempt))
        coVerify(exactly = 0) { coordinator.refresh(any()) }
    }

    @Test
    fun `missing authorization header refreshes without a stale token`() {
        coEvery { coordinator.refresh(null) } returns
            TokenRefreshResult.Refreshed(session(accessToken = "fresh"))

        val replayed =
            authenticator.authenticate(
                null,
                response(requestToken = null),
            )

        assertEquals("Bearer fresh", replayed?.header("Authorization"))
        coVerify { coordinator.refresh(null) }
    }

    private fun session(accessToken: String): LoginSession =
        LoginSession(
            accessToken = accessToken,
            refreshToken = "refresh",
            userId = "user1",
            username = "shubh",
            baseUrl = "https://host",
        )

    private fun request(requestToken: String?): Request =
        Request
            .Builder()
            .url("https://host/api/libraries")
            .apply {
                if (requestToken != null) {
                    header("Authorization", "Bearer $requestToken")
                }
            }.build()

    private fun response(
        requestToken: String?,
        priorResponse: Response? = null,
    ): Response =
        Response
            .Builder()
            .request(request(requestToken))
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .priorResponse(priorResponse)
            .build()
}
