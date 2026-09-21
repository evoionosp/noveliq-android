package org.evoionosp.noveliq.data.network

import android.content.Context
import dagger.Lazy
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.evoionosp.noveliq.data.test.FakeConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedOkHttpClientsTest {
    private val store = FakeConnectionSettingsStore()

    private fun newHolder(): ManagedOkHttpClients =
        ManagedOkHttpClients(
            context = mockk<Context>(),
            connectionSettingsStore = store,
            tokenAuthenticator = Lazy { mockk<TokenAuthenticator>() },
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `clients carry the headers interceptor`() {
        val holder = newHolder()

        assertTrue(holder.apiClient.interceptors.any { it is ConnectionHeadersInterceptor })
        assertTrue(holder.authClient.interceptors.any { it is ConnectionHeadersInterceptor })
    }

    @Test
    fun `security change rebuilds both clients`() {
        val holder = newHolder()
        val firstApi = holder.apiClient
        val firstAuth = holder.authClient

        emit(storeCurrent().copy(bypassSsl = true))

        assertNotSame(firstApi, holder.apiClient)
        assertNotSame(firstAuth, holder.authClient)
    }

    @Test
    fun `headers only change keeps existing clients`() {
        val holder = newHolder()
        val firstApi = holder.apiClient

        emit(storeCurrent().copy(customHeaders = listOf(ServerRequestHeader("X-K", "v"))))

        assertSame(firstApi, holder.apiClient)
    }

    @Test
    fun `rebuild runs registered invalidators once per security change`() {
        val holder = newHolder()
        var invalidations = 0
        holder.registerInvalidator { invalidations++ }

        emit(storeCurrent().copy(bypassSsl = true))
        emit(storeCurrent().copy(userAgent = "Other/1.0"))

        assertEquals(1, invalidations)
    }

    private fun storeCurrent(): ConnectionSettings = runBlocking { store.settings.first() }

    private fun emit(settings: ConnectionSettings) {
        store.emit(settings)
    }
}
