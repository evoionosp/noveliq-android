package org.evoionosp.noveliq.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.session.LoginSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun preferences(): SharedPreferences = context.getSharedPreferences("test_session_store", Context.MODE_PRIVATE)

    private fun newStore(): SessionDataStore = SessionDataStore(preferences())

    @After
    fun tearDown() {
        preferences().edit().clear().commit()
    }

    @Test
    fun `saveSession persists tokens and derives expiry from the jwt`() =
        runTest {
            val store = newStore()
            val expiry = 1_999_999_999L
            val session =
                LoginSession(
                    accessToken = jwt(expiresAt = expiry),
                    refreshToken = "refresh",
                    userId = "user1",
                    username = "shubh",
                    baseUrl = "https://host",
                )

            val stored = store.saveSession(session)

            assertEquals(expiry, stored.accessTokenExpiresAtEpochSeconds)
            newStore().session.test {
                assertEquals(stored, awaitItem())
            }
        }

    @Test
    fun `saveSession stores opaque tokens without expiry`() =
        runTest {
            val store = newStore()
            val session =
                LoginSession(
                    accessToken = "opaque-token",
                    refreshToken = null,
                    userId = null,
                    username = "shubh",
                    baseUrl = "https://host",
                )

            val stored = store.saveSession(session)

            assertNull(stored.accessTokenExpiresAtEpochSeconds)
            newStore().session.test {
                val restored = awaitItem()
                assertEquals("opaque-token", restored?.accessToken)
                assertNull(restored?.refreshToken)
            }
        }

    @Test
    fun `session flow emits updates and null after clear`() =
        runTest {
            val store = newStore()
            store.session.test {
                assertNull(awaitItem())

                val stored = store.saveSession(session())
                assertEquals(stored, awaitItem())

                store.clearSession()
                assertNull(awaitItem())
            }
        }

    @Test
    fun `clearSession preserves the last server url`() =
        runTest {
            val store = newStore()
            store.saveSession(session())

            store.clearSession()

            store.lastServerUrl.test {
                assertEquals("https://host", awaitItem())
            }
            newStore().session.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun `structurally invalid sessions are not restored`() =
        runTest {
            val store = newStore()
            store.saveSession(session().copy(baseUrl = ""))

            newStore().session.test {
                assertNull(awaitItem())
            }
        }

    private fun session(): LoginSession =
        LoginSession(
            accessToken = "opaque-token",
            refreshToken = "refresh",
            userId = "user1",
            username = "shubh",
            baseUrl = "https://host",
        )

    private fun jwt(expiresAt: Long): String {
        val payload =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                """{"exp": $expiresAt}""".toByteArray(Charsets.UTF_8),
            )
        return "eyJhbGciOiJIUzI1NiJ9.$payload.sig"
    }
}
