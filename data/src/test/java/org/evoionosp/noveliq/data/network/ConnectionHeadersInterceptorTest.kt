package org.evoionosp.noveliq.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.RecordedRequest
import org.evoionosp.noveliq.data.test.FakeConnectionSettingsStore
import org.evoionosp.noveliq.data.test.MockWebServerRule
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ConnectionHeadersInterceptorTest {
    @get:Rule
    val mockServer = MockWebServerRule()

    private val store = FakeConnectionSettingsStore()
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor(ConnectionHeadersInterceptor(store))
            .build()

    @Test
    fun `applies user agent`() {
        val recorded = get()

        assertEquals("FakeAgent/1.0", recorded.getHeader("User-Agent"))
    }

    @Test
    fun `custom headers reach the server`() {
        runBlocking { store.setCustomHeaders(listOf(ServerRequestHeader("X-Proxy-Token", "secret"))) }

        val recorded = get()

        assertEquals("secret", recorded.getHeader("X-Proxy-Token"))
        assertEquals("FakeAgent/1.0", recorded.getHeader("User-Agent"))
    }

    @Test
    fun `configured agent wins over a custom user agent row`() {
        runBlocking { store.setCustomHeaders(listOf(ServerRequestHeader("User-Agent", "CustomRow/9.9"))) }

        val recorded = get()

        assertEquals("FakeAgent/1.0", recorded.getHeader("User-Agent"))
    }

    @Test
    fun `custom authorization row is skipped`() {
        runBlocking { store.setCustomHeaders(listOf(ServerRequestHeader("Authorization", "Bearer rogue"))) }

        val recorded = get()

        assertNull(recorded.getHeader("Authorization"))
    }

    private fun get(): RecordedRequest {
        mockServer.enqueuePlainText(200, "ok")
        val request = Request.Builder().url(mockServer.baseUrl()).build()
        client.newCall(request).execute().close()
        return mockServer.takeRequest()
    }
}
