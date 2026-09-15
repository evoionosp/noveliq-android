package org.evoionosp.noveliq.domain.session.usecase

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.auth.FakeSessionStore
import org.evoionosp.noveliq.domain.session.CoverArtCache
import org.evoionosp.noveliq.domain.session.DownloadStore
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler
import org.evoionosp.noveliq.domain.session.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionUseCasesTest {
    private val session =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    @Test
    fun `save and observe round-trip the stored session`() =
        runTest {
            val store = FakeSessionStore(null)

            val stored = SaveSessionUseCase(store)(session)

            assertEquals(session, stored)
            ObserveSessionUseCase(store)().test {
                assertEquals(session, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(session, GetCurrentSessionUseCase(store)())
        }

    @Test
    fun `get current session returns null when signed out`() =
        runTest {
            assertNull(GetCurrentSessionUseCase(FakeSessionStore(null))())
        }

    @Test
    fun `logout ends the stored session but keeps the last server url`() =
        runTest {
            val store = FakeSessionStore(session)

            logoutUseCase(store)()

            assertNull(store.session.first())
            assertEquals("https://example.com", store.lastServerUrl.first())
        }

    @Test
    fun `observe last server url emits the most recent server`() =
        runTest {
            val store = FakeSessionStore(session)

            ObserveLastServerUrlUseCase(store)().test {
                assertEquals("https://example.com", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun logoutUseCase(sessionStore: SessionStore) =
        LogoutUserUseCase(
            playerLogoutHandler = PlayerLogoutHandler { },
            sessionStore = sessionStore,
            downloadStore = DownloadStore { },
            coverArtCache = CoverArtCache { },
            localCatalogCleaner = LocalCatalogCleaner { },
        )
}
