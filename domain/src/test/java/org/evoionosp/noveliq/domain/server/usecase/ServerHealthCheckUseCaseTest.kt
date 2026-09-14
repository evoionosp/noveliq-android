package org.evoionosp.noveliq.domain.server.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.testing.FakeServerRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerHealthCheckUseCaseTest {
    @Test
    fun `delegates a healthy result to the repository`() =
        runTest {
            val useCase =
                ServerHealthCheckUseCase(
                    FakeServerRepository(healthResult = ServerCheckResult.Success(true)),
                )

            assertEquals(ServerCheckResult.Success(true), useCase("https://example.com"))
        }

    @Test
    fun `passes a health check failure through unchanged`() =
        runTest {
            val expected = ServerCheckResult.Failure(ServerError.HEALTHCHECK_FAILED)
            val useCase =
                ServerHealthCheckUseCase(
                    FakeServerRepository(healthResult = expected),
                )

            assertEquals(expected, useCase("https://example.com"))
        }
}
