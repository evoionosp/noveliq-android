package org.evoionosp.noveliq.presentation.settings

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.noveliq.domain.session.CoverArtCache
import org.evoionosp.noveliq.domain.session.DownloadStore
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler
import org.evoionosp.noveliq.domain.session.SessionStore
import org.evoionosp.noveliq.domain.session.usecase.LogoutUserUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {
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
    fun `logout runs the flow exposes progress and completes`() =
        runTest {
            val sessionStore = FakeSessionStore(signedInSession())
            val viewModel = PreferencesViewModel(logoutUseCase(sessionStore))
            var completed = false

            viewModel.isLoggingOut.test {
                assertEquals(false, awaitItem())

                viewModel.logout { completed = true }
                runCurrent()
                assertEquals(true, awaitItem())

                advanceUntilIdle()
                assertEquals(false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertTrue(completed)
            assertNull(sessionStore.session.first())
        }

    @Test
    fun `second logout while running is ignored`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            var invocations = 0
            val useCase =
                logoutUseCase(
                    FakeSessionStore(signedInSession()),
                    playerLogoutHandler =
                        PlayerLogoutHandler {
                            invocations++
                            gate.await()
                        },
                )
            val viewModel = PreferencesViewModel(useCase)

            viewModel.logout { }
            runCurrent()
            viewModel.logout { }
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, invocations)
        }

    private fun logoutUseCase(
        sessionStore: SessionStore,
        playerLogoutHandler: PlayerLogoutHandler = PlayerLogoutHandler { },
    ) = LogoutUserUseCase(
        playerLogoutHandler = playerLogoutHandler,
        sessionStore = sessionStore,
        downloadStore = DownloadStore { },
        coverArtCache = CoverArtCache { },
        localCatalogCleaner = LocalCatalogCleaner { },
    )

    private fun signedInSession() =
        LoginSession(
            accessToken = "access",
            refreshToken = "refresh",
            userId = "user-1",
            username = "noveliq",
            baseUrl = "https://example.com",
            accessTokenExpiresAtEpochSeconds = null,
        )

    private class FakeSessionStore(
        initialSession: LoginSession?,
    ) : SessionStore {
        private val backingFlow = MutableStateFlow(initialSession)

        override val session: Flow<LoginSession?> = backingFlow
        override val lastServerUrl: Flow<String?> = MutableStateFlow(initialSession?.baseUrl)

        override suspend fun saveSession(session: LoginSession): LoginSession {
            backingFlow.value = session
            return session
        }

        override suspend fun clearSession() {
            backingFlow.value = null
        }
    }
}
