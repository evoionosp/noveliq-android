package org.evoionosp.noveliq.data.auth.repository

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.auth.remote.api.LoginServiceFactory
import org.evoionosp.noveliq.data.test.MockWebServerRule
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val testDispatcher = StandardTestDispatcher()
    private val repository =
        AuthRepositoryImpl(
            serviceFactory = LoginServiceFactory(OkHttpClient()),
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `login posts credentials and maps top-level tokens`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"access_token": "a", "refresh_token": "r", "user_id": "u"}""",
            )

            val result = repository.login(serverRule.baseUrl(), "user", "pass")

            assertEquals(LoginResult.Success(LoginData("a", "r", "u")), result)
            val requestBody = serverRule.takeRequest().body.readUtf8()
            assertTrue(requestBody.contains("\"username\":\"user\""))
            assertTrue(requestBody.contains("\"password\":\"pass\""))
        }

    @Test
    fun `login falls back to nested user tokens`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"user": {"token": "t", "refreshToken": "rt", "id": "i"}}""",
            )

            assertEquals(
                LoginResult.Success(LoginData("t", "rt", "i")),
                repository.login(serverRule.baseUrl(), "user", "pass"),
            )
        }

    @Test
    fun `login maps http network and invalid url failures`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(401, "{}")
            assertEquals(
                LoginResult.Failure(AuthError.HTTP, 401),
                repository.login(serverRule.baseUrl(), "user", "wrong"),
            )

            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                LoginResult.Failure(AuthError.NETWORK),
                repository.login(deadUrl, "user", "pass"),
            )

            assertEquals(
                LoginResult.Failure(AuthError.INVALID_BASE_URL),
                repository.login("", "user", "pass"),
            )
        }

    @Test
    fun `refreshSession maps success and http failures`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, """{"access_token": "fresh"}""")
            assertEquals(
                LoginResult.Success(LoginData("fresh", null, null)),
                repository.refreshSession(serverRule.baseUrl(), "old-refresh"),
            )

            serverRule.enqueueJson(401, "{}")
            assertEquals(
                LoginResult.Failure(AuthError.HTTP, 401),
                repository.refreshSession(serverRule.baseUrl(), "dead-refresh"),
            )
        }

    @Test
    fun `refreshSession maps network and invalid url failures`() =
        runTest(testDispatcher) {
            assertEquals(
                LoginResult.Failure(AuthError.INVALID_BASE_URL),
                repository.refreshSession("", "refresh"),
            )

            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                LoginResult.Failure(AuthError.NETWORK),
                repository.refreshSession(deadUrl, "refresh"),
            )
        }
}
