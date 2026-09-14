package org.evoionosp.noveliq.domain.server.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.evoionosp.noveliq.domain.testing.FakeServerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPingUseCaseTest {
    private val status =
        ServerStatus(
            app = "audiobookshelf",
            serverVersion = "2.0.0",
            isInit = true,
            language = "en-US",
            authMethods = listOf("local"),
            authLoginCustomMessage = "",
        )

    @Test
    fun `returns the server status when ping and status both succeed`() =
        runTest {
            val repository =
                FakeServerRepository(
                    pingResult = ServerCheckResult.Success(true),
                    statusResult = ServerCheckResult.Success(status),
                )

            val result = ServerPingUseCase(repository)("https://example.com")

            assertEquals(ServerCheckResult.Success(status), result)
        }

    @Test
    fun `passes a ping failure through without fetching status`() =
        runTest {
            val repository =
                FakeServerRepository(
                    pingResult = ServerCheckResult.Failure(ServerError.NETWORK),
                )

            val result = ServerPingUseCase(repository)("https://example.com")

            assertEquals(ServerCheckResult.Failure(ServerError.NETWORK), result)
            assertEquals(0, repository.statusCallCount)
        }

    @Test
    fun `maps a negative ping to PING_FAILED without fetching status`() =
        runTest {
            val repository =
                FakeServerRepository(
                    pingResult = ServerCheckResult.Success(false),
                )

            val result = ServerPingUseCase(repository)("https://example.com")

            assertEquals(ServerCheckResult.Failure(ServerError.PING_FAILED), result)
            assertEquals(0, repository.statusCallCount)
        }

    @Test
    fun `passes a status failure through after a successful ping`() =
        runTest {
            val repository =
                FakeServerRepository(
                    pingResult = ServerCheckResult.Success(true),
                    statusResult = ServerCheckResult.Failure(ServerError.HTTP, code = 500),
                )

            val result = ServerPingUseCase(repository)("https://example.com")

            assertTrue(result is ServerCheckResult.Failure)
            assertEquals(ServerError.HTTP, (result as ServerCheckResult.Failure).error)
        }
}
