package org.evoionosp.noveliq.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidConnectivityObserverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val observer = AndroidConnectivityObserver(context)

    @After
    fun tearDown() {
        val shadow = shadowOf(context.getSystemService(ConnectivityManager::class.java))
        shadow.setDefaultNetworkActive(false)
        shadow.setActiveNetworkInfo(null)
    }

    @Test
    fun `disconnected by default`() {
        assertFalse(observer.isConnected())
    }

    @Test
    fun `connected wifi reports connected`() {
        setConnected(true)

        assertTrue(observer.isConnected())
    }

    @Test
    fun `observe emits the current state on collection`() =
        runTest {
            observer.observe().test {
                assertEquals(false, awaitItem())
            }

            setConnected(true)
            observer.observe().test {
                assertEquals(true, awaitItem())
            }
        }

    @Test
    fun `observe follows network callbacks`() =
        runTest {
            // Must match the active NetworkInfo type: the shadow looks the
            // active Network up by type, not by an arbitrary id.
            val network = ShadowNetwork.newInstance(ConnectivityManager.TYPE_WIFI)
            observer.observe().test {
                assertEquals(false, awaitItem())

                callbacks().forEach { it.onAvailable(network) }
                assertEquals(true, awaitItem())

                callbacks().forEach { it.onLost(network) }
                assertEquals(false, awaitItem())
            }
        }

    private fun setConnected(connected: Boolean) {
        val shadow =
            shadowOf(context.getSystemService(ConnectivityManager::class.java))
        if (connected) {
            val info =
                ShadowNetworkInfo.newInstance(
                    NetworkInfo.DetailedState.CONNECTED,
                    ConnectivityManager.TYPE_WIFI,
                    0,
                    true,
                    true,
                )
            val network = ShadowNetwork.newInstance(ConnectivityManager.TYPE_WIFI)
            shadow.setDefaultNetworkActive(true)
            shadow.setActiveNetworkInfo(info)
            shadow.addNetwork(network, info)
            // NetworkCapabilities.Builder is stripped from the compile-SDK stub
            // jar, so capabilities are built through the Robolectric shadow instead.
            val capabilities = ShadowNetworkCapabilities.newInstance()
            shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadow.setNetworkCapabilities(network, capabilities)
        } else {
            shadowOf(context.getSystemService(ConnectivityManager::class.java))
                .setActiveNetworkInfo(null)
        }
    }

    private fun callbacks(): Set<ConnectivityManager.NetworkCallback> =
        shadowOf(context.getSystemService(ConnectivityManager::class.java)).networkCallbacks
}
