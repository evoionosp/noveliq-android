package org.evoionosp.noveliq.domain.auth.usecase

import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.auth.FakeAuthRepository
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginUseCaseTest {
    @Test
    fun `delegates a successful login to the repository`() =
        runTest {
            val expected = LoginResult.Success(LoginData("access", "refresh", "user-1"))
            val useCase = LoginUseCase(FakeAuthRepository(expected))

            val result = useCase("https://example.com", "demo", "secret")

            assertEquals(expected, result)
        }

    @Test
    fun `passes a login failure through unchanged`() =
        runTest {
            val expected = LoginResult.Failure(AuthError.NETWORK)
            val useCase = LoginUseCase(FakeAuthRepository(expected))

            val result = useCase("https://example.com", "demo", "secret")

            assertEquals(expected, result)
        }
}
