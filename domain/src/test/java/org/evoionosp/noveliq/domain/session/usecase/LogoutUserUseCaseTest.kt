package org.evoionosp.noveliq.domain.session.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.session.CoverArtCache
import org.evoionosp.noveliq.domain.session.DownloadStore
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.LogoutStep
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler
import org.evoionosp.noveliq.domain.session.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LogoutUserUseCaseTest {
    private val invocations = mutableListOf<LogoutStep>()

    private val playerLogoutHandler =
        object : PlayerLogoutHandler {
            var failWith: Exception? = null

            override suspend fun onLogout() {
                invocations += LogoutStep.STOP_PLAYBACK
                failWith?.let { throw it }
            }
        }

    private val sessionStore =
        object : SessionStore {
            private val sessionState = MutableStateFlow<LoginSession?>(signedInSession())
            override val session: Flow<LoginSession?> = sessionState
            override val lastServerUrl: Flow<String?> = MutableStateFlow("https://example.com")

            override suspend fun saveSession(session: LoginSession): LoginSession {
                sessionState.value = session
                return session
            }

            override suspend fun clearSession() {
                invocations += LogoutStep.CLEAR_SESSION
                sessionState.value = null
            }
        }

    private val downloadStore =
        object : DownloadStore {
            override suspend fun deleteAll() {
                invocations += LogoutStep.DELETE_DOWNLOADS
            }
        }

    private val coverArtCache =
        object : CoverArtCache {
            var failWith: Exception? = null

            override suspend fun clear() {
                invocations += LogoutStep.CLEAR_COVER_ART_CACHE
                failWith?.let { throw it }
            }
        }

    private val localCatalogCleaner =
        object : LocalCatalogCleaner {
            override suspend fun clear() {
                invocations += LogoutStep.CLEAR_CATALOG_CACHE
            }
        }

    private val useCase =
        LogoutUserUseCase(
            playerLogoutHandler = playerLogoutHandler,
            sessionStore = sessionStore,
            downloadStore = downloadStore,
            coverArtCache = coverArtCache,
            localCatalogCleaner = localCatalogCleaner,
        )

    @Test
    fun `runs logout steps in order`() =
        runTest {
            val result = useCase()

            assertEquals(
                listOf(
                    LogoutStep.STOP_PLAYBACK,
                    LogoutStep.CLEAR_SESSION,
                    LogoutStep.DELETE_DOWNLOADS,
                    LogoutStep.CLEAR_COVER_ART_CACHE,
                    LogoutStep.CLEAR_CATALOG_CACHE,
                ),
                invocations,
            )
            assertTrue(result.isFullyClean)
            assertNull(sessionStore.session.first())
        }

    @Test
    fun `runs remaining steps and reports failures when a step throws`() =
        runTest {
            playerLogoutHandler.failWith = RuntimeException("player boom")
            coverArtCache.failWith = RuntimeException("cache boom")

            val result = useCase()

            // Fail-open: every step still ran, including the session clear.
            assertEquals(LogoutStep.values().toList(), invocations)
            assertEquals(
                listOf(LogoutStep.STOP_PLAYBACK, LogoutStep.CLEAR_COVER_ART_CACHE),
                result.failedSteps,
            )
            assertNull(sessionStore.session.first())
        }

    @Test
    fun `propagates cancellation instead of recording it`() =
        runTest {
            playerLogoutHandler.failWith = CancellationException("cancelled")

            try {
                useCase()
                fail("expected CancellationException")
            } catch (_: CancellationException) {
                // Expected: a cancelled logout stays cancelled.
            }
        }

    private fun signedInSession() =
        LoginSession(
            accessToken = "access",
            refreshToken = "refresh",
            userId = "user-1",
            username = "noveliq",
            baseUrl = "https://example.com",
            accessTokenExpiresAtEpochSeconds = null,
        )
}
