package org.evoionosp.noveliq.data.connection

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionSettingsDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val defaultUserAgent = "TestAgent/1.0"

    private fun preferences(): SharedPreferences = context.getSharedPreferences("test_connection_store", Context.MODE_PRIVATE)

    private fun newStore(): ConnectionSettingsDataStore = ConnectionSettingsDataStore(preferences(), Gson(), defaultUserAgent)

    @After
    fun tearDown() {
        preferences().edit().clear().commit()
    }

    @Test
    fun `fresh store reports defaults`() =
        runTest {
            newStore().settings.test {
                val settings = awaitItem()
                assertTrue(settings.customHeaders.isEmpty())
                assertFalse(settings.bypassSsl)
                assertNull(settings.clientCertificateAlias)
                assertEquals(defaultUserAgent, settings.userAgent)
            }
        }

    @Test
    fun `setters persist and survive a new store instance`() =
        runTest {
            val store = newStore()
            store.setCustomHeaders(listOf(ServerRequestHeader("X-Key", "v")))
            store.setBypassSsl(true)
            store.setClientCertificateAlias("alias-1")
            store.setUserAgent("CustomAgent/2.0")

            newStore().settings.test {
                val settings = awaitItem()
                assertEquals(1, settings.customHeaders.size)
                assertEquals("X-Key", settings.customHeaders[0].name)
                assertTrue(settings.bypassSsl)
                assertEquals("alias-1", settings.clientCertificateAlias)
                assertEquals("CustomAgent/2.0", settings.userAgent)
            }
        }

    @Test
    fun `setCustomHeaders sanitizes before persisting`() =
        runTest {
            newStore().setCustomHeaders(
                listOf(
                    ServerRequestHeader("X-Key", "first"),
                    ServerRequestHeader("X-Key", "second"),
                    ServerRequestHeader("  ", "v"),
                ),
            )

            newStore().settings.test {
                val headers = awaitItem().customHeaders
                assertEquals(1, headers.size)
                assertEquals("first", headers[0].value)
            }
        }

    @Test
    fun `blank user agent falls back to default`() =
        runTest {
            val store = newStore()
            store.setUserAgent("   ")

            store.settings.test {
                assertEquals(defaultUserAgent, awaitItem().userAgent)
            }
        }

    @Test
    fun `resetUserAgent restores default`() =
        runTest {
            val store = newStore()
            store.setUserAgent("CustomAgent/2.0")
            store.resetUserAgent()

            store.settings.test {
                assertEquals(defaultUserAgent, awaitItem().userAgent)
            }
        }

    @Test
    fun `blank alias clears the stored alias`() =
        runTest {
            val store = newStore()
            store.setClientCertificateAlias("alias-1")
            store.setClientCertificateAlias("  ")

            store.settings.test {
                assertNull(awaitItem().clientCertificateAlias)
            }
        }
}
