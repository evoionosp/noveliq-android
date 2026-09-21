package org.evoionosp.noveliq.data.test

import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.network.ManagedOkHttpClients

/**
 * Returns [ManagedOkHttpClients] backed by a plain client for MockWebServer tests.
 *
 * Repository tests exercise HTTP behavior through MockWebServer, not the managed
 * client's rebuild logic, so both the API and auth clients are the same bare
 * [OkHttpClient] and cache invalidation is a no-op.
 */
fun testManagedClients(client: OkHttpClient = OkHttpClient()): ManagedOkHttpClients {
    val clients = mockk<ManagedOkHttpClients>()
    every { clients.apiClient } returns client
    every { clients.authClient } returns client
    every { clients.registerInvalidator(any()) } returns Unit
    return clients
}
