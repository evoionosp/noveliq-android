package org.evoionosp.noveliq.data.server.repository

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.evoionosp.noveliq.data.server.remote.api.ServerCheckServiceFactory
import org.evoionosp.noveliq.data.test.MockWebServerRule
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ServerRepositoryImplTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val testDispatcher = StandardTestDispatcher()
    private val repository =
        ServerRepositoryImpl(
            serviceFactory = ServerCheckServiceFactory(OkHttpClient()),
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `ping maps success flag and http failures`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(200, """{"success": true}""")
            assertEquals(
                ServerCheckResult.Success(true),
                repository.ping(serverRule.baseUrl()),
            )

            serverRule.enqueueJson(500, "{}")
            assertEquals(
                ServerCheckResult.Failure(ServerError.HTTP, 500),
                repository.ping(serverRule.baseUrl()),
            )
        }

    @Test
    fun `getStatus maps the server status`() =
        runTest(testDispatcher) {
            serverRule.enqueueJson(
                200,
                """{"app": "audiobookshelf", "serverVersion": "2.0.0", "isInit": true,
                    "language": "en", "authMethods": ["local"],
                    "authFormData": {"authLoginCustomMessage": "Hi"}}""",
            )

            assertEquals(
                ServerCheckResult.Success(
                    ServerStatus(
                        app = "audiobookshelf",
                        serverVersion = "2.0.0",
                        isInit = true,
                        language = "en",
                        authMethods = listOf("local"),
                        authLoginCustomMessage = "Hi",
                    ),
                ),
                repository.getStatus(serverRule.baseUrl()),
            )
        }

    @Test
    fun `healthCheck accepts ok case-insensitively`() =
        runTest(testDispatcher) {
            serverRule.enqueuePlainText(200, "OK")
            assertEquals(
                ServerCheckResult.Success(true),
                repository.healthCheck(serverRule.baseUrl()),
            )

            serverRule.enqueuePlainText(200, "ok")
            assertEquals(
                ServerCheckResult.Success(true),
                repository.healthCheck(serverRule.baseUrl()),
            )
        }

    @Test
    fun `healthCheck rejects unexpected bodies and failed responses`() =
        runTest(testDispatcher) {
            serverRule.enqueuePlainText(200, "DOWN")
            assertEquals(
                ServerCheckResult.Failure(ServerError.HEALTHCHECK_UNEXPECTED),
                repository.healthCheck(serverRule.baseUrl()),
            )

            serverRule.enqueuePlainText(500, "DOWN")
            assertEquals(
                ServerCheckResult.Failure(ServerError.HEALTHCHECK_FAILED, 500),
                repository.healthCheck(serverRule.baseUrl()),
            )
        }

    @Test
    fun `healthCheck maps transport and invalid url failures`() =
        runTest(testDispatcher) {
            assertEquals(
                ServerCheckResult.Failure(ServerError.INVALID_BASE_URL),
                repository.healthCheck(""),
            )

            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                ServerCheckResult.Failure(ServerError.NETWORK),
                repository.healthCheck(deadUrl),
            )
        }

    @Test
    fun `healthCheck maps http failures from the service`() =
        runTest(testDispatcher) {
            val failingFactory = mockk<ServerCheckServiceFactory>()
            every { failingFactory.create(any()) } throws
                HttpException(Response.error<String>(500, "".toResponseBody("text/plain".toMediaType())))
            val failingRepository =
                ServerRepositoryImpl(
                    serviceFactory = failingFactory,
                    ioDispatcher = testDispatcher,
                )

            assertEquals(
                ServerCheckResult.Failure(ServerError.HTTP, 500),
                failingRepository.healthCheck(serverRule.baseUrl()),
            )
        }

    @Test
    fun `repositories map network and invalid url failures`() =
        runTest(testDispatcher) {
            val deadUrl = serverRule.baseUrl()
            serverRule.server.shutdown()
            assertEquals(
                ServerCheckResult.Failure(ServerError.NETWORK),
                repository.ping(deadUrl),
            )

            assertEquals(
                ServerCheckResult.Failure(ServerError.INVALID_BASE_URL),
                repository.getStatus(""),
            )
        }
}
