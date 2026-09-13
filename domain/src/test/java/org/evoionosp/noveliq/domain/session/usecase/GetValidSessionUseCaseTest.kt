package org.evoionosp.noveliq.domain.session.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.auth.FakeAuthRepository
import org.evoionosp.noveliq.domain.auth.FakeSessionStore
import org.evoionosp.noveliq.domain.auth.SessionRefreshCoordinator
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.session.LoginSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetValidSessionUseCaseTest {
    @Test
    fun `returns the stored session untouched when the token has not expired`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = 3600))
            val authRepository =
                FakeAuthRepository(
                    LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")),
                )

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertEquals("access-1", result?.accessToken)
            assertEquals(0, authRepository.refreshCallCount)
        }

    /**
     * The cold-start case after the app has been closed for longer than the token's lifetime:
     * the token has to be rotated before the first request rather than after it 401s.
     */
    @Test
    fun `refreshes up front when the stored token has already expired`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = -3600))
            val authRepository =
                FakeAuthRepository(
                    LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")),
                )

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertEquals("new-access", result?.accessToken)
            assertEquals(1, authRepository.refreshCallCount)
        }

    @Test
    fun `refreshes when the token is inside the expiry skew window`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = 5))
            val authRepository =
                FakeAuthRepository(
                    LoginResult.Success(LoginData("new-access", "refresh-2", "user-1")),
                )

            GetValidSessionUseCase(store, SessionRefreshCoordinator(store, authRepository))()

            assertEquals(1, authRepository.refreshCallCount)
        }

    @Test
    fun `returns null when the expired session cannot be recovered`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = -3600))
            val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.HTTP, code = 401))

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertNull(result)
            assertNull(store.session.first())
        }

    @Test
    fun `hands back the expired session when the refresh cannot reach the server`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = -3600))
            val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.NETWORK))

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertEquals("access-1", result?.accessToken)
        }

    @Test
    fun `treats an unknown expiry as usable`() =
        runTest {
            val store = FakeSessionStore(session(expiresInSeconds = null))
            val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.HTTP, code = 401))

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertEquals("access-1", result?.accessToken)
            assertEquals(0, authRepository.refreshCallCount)
        }

    @Test
    fun `returns null when there is no session at all`() =
        runTest {
            val store = FakeSessionStore(null)
            val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.NETWORK))

            val result =
                GetValidSessionUseCase(
                    store,
                    SessionRefreshCoordinator(store, authRepository),
                )()

            assertNull(result)
        }

    private fun session(expiresInSeconds: Long?): LoginSession {
        val nowEpochSeconds = System.currentTimeMillis() / 1000
        return LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
            accessTokenExpiresAtEpochSeconds = expiresInSeconds?.let { nowEpochSeconds + it },
        )
    }
}
