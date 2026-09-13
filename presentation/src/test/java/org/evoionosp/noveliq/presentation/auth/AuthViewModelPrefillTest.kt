package org.evoionosp.noveliq.presentation.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.auth.usecase.LoginUseCase
import org.evoionosp.noveliq.domain.server.model.ServerCheckResult
import org.evoionosp.noveliq.domain.server.model.ServerError
import org.evoionosp.noveliq.domain.server.model.ServerStatus
import org.evoionosp.noveliq.domain.server.repository.ServerRepository
import org.evoionosp.noveliq.domain.server.usecase.ServerHealthCheckUseCase
import org.evoionosp.noveliq.domain.server.usecase.ServerPingUseCase
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore
import org.evoionosp.noveliq.domain.session.usecase.ObserveLastServerUrlUseCase
import org.evoionosp.noveliq.domain.session.usecase.SaveSessionUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelPrefillTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prefills the last used https server`() =
        runTest(dispatcher) {
            val viewModel = viewModel(lastServerUrl = "https://books.example.com")

            advanceUntilIdle()

            assertEquals("https://", viewModel.uiState.value.protocol)
            assertEquals("books.example.com", viewModel.uiState.value.baseUrl)
        }

    @Test
    fun `prefills the protocol that was used last`() =
        runTest(dispatcher) {
            val viewModel = viewModel(lastServerUrl = "http://192.168.1.10:13378")

            advanceUntilIdle()

            assertEquals("http://", viewModel.uiState.value.protocol)
            assertEquals("192.168.1.10:13378", viewModel.uiState.value.baseUrl)
        }

    @Test
    fun `leaves the form empty when no server has been used yet`() =
        runTest(dispatcher) {
            val viewModel = viewModel(lastServerUrl = null)

            advanceUntilIdle()

            assertEquals("https://", viewModel.uiState.value.protocol)
            assertEquals("", viewModel.uiState.value.baseUrl)
        }

    @Test
    fun `does not overwrite a url the user already typed`() =
        runTest(dispatcher) {
            val viewModel = viewModel(lastServerUrl = "https://books.example.com")

            viewModel.onBaseUrlChange("other.example.com")
            advanceUntilIdle()

            assertEquals("other.example.com", viewModel.uiState.value.baseUrl)
        }

    @Test
    fun `splits a pasted protocol regardless of case`() =
        runTest(dispatcher) {
            val viewModel = viewModel(lastServerUrl = null)
            advanceUntilIdle()

            viewModel.onBaseUrlChange("HTTP://books.example.com")

            assertEquals("http://", viewModel.uiState.value.protocol)
            assertEquals("books.example.com", viewModel.uiState.value.baseUrl)
        }

    private fun viewModel(lastServerUrl: String?): AuthViewModel {
        val sessionStore = PrefillFakeSessionStore(lastServerUrl)
        val serverRepository = UnusedServerRepository()
        val authRepository = UnusedAuthRepository()
        return AuthViewModel(
            serverPingUseCase = ServerPingUseCase(serverRepository),
            serverHealthCheckUseCase = ServerHealthCheckUseCase(serverRepository),
            loginUseCase = LoginUseCase(authRepository),
            saveSessionUseCase = SaveSessionUseCase(sessionStore),
            observeLastServerUrlUseCase = ObserveLastServerUrlUseCase(sessionStore),
        )
    }
}

private class PrefillFakeSessionStore(
    lastServerUrl: String?,
) : SessionStore {
    private val sessionFlow = MutableStateFlow<LoginSession?>(null)
    private val lastServerFlow = MutableStateFlow(lastServerUrl)

    override val session: Flow<LoginSession?> = sessionFlow

    override val lastServerUrl: Flow<String?> = lastServerFlow

    override suspend fun saveSession(session: LoginSession): LoginSession {
        sessionFlow.value = session
        lastServerFlow.value = session.baseUrl
        return session
    }

    override suspend fun clearSession() {
        sessionFlow.value = null
    }
}

private class UnusedServerRepository : ServerRepository {
    override suspend fun ping(baseUrl: String): ServerCheckResult<Boolean> = ServerCheckResult.Failure(ServerError.UNKNOWN)

    override suspend fun getStatus(baseUrl: String): ServerCheckResult<ServerStatus> = ServerCheckResult.Failure(ServerError.UNKNOWN)

    override suspend fun healthCheck(baseUrl: String): ServerCheckResult<Boolean> = ServerCheckResult.Failure(ServerError.UNKNOWN)
}

private class UnusedAuthRepository : AuthRepository {
    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): LoginResult = LoginResult.Failure(AuthError.UNEXPECTED)

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String,
    ): LoginResult = LoginResult.Failure(AuthError.UNEXPECTED)
}
