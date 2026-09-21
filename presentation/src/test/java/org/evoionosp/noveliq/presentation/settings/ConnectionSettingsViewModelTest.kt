package org.evoionosp.noveliq.presentation.settings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionSettingsViewModelTest {
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
    fun `uiState maps stored settings`() =
        runTest {
            val store =
                FakeConnectionSettingsStore(
                    ConnectionSettings(
                        customHeaders = listOf(ServerRequestHeader(name = "X-Api-Key", value = "secret", id = "h1")),
                        bypassSsl = true,
                        clientCertificateAlias = "alias-1",
                        userAgent = "TestAgent/1.0",
                    ),
                )
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.uiState.test {
                // stateIn replays its initial value before the mapped settings arrive.
                assertEquals(ConnectionSettingsUiState(), awaitItem())
                val state = awaitItem()
                assertEquals(listOf(ServerRequestHeader(name = "X-Api-Key", value = "secret", id = "h1")), state.headers)
                assertTrue(state.bypassSsl)
                assertEquals("alias-1", state.clientCertificateAlias)
                assertEquals("TestAgent/1.0", state.userAgent)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saveHeader appends a new header`() =
        runTest {
            val store = FakeConnectionSettingsStore()
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.saveHeader(ServerRequestHeader(name = "X-Api-Key", value = "secret", id = "h1"))
            advanceUntilIdle()

            assertEquals(
                listOf(ServerRequestHeader(name = "X-Api-Key", value = "secret", id = "h1")),
                store.settings.first().customHeaders,
            )
        }

    @Test
    fun `saveHeader replaces the header with the same id`() =
        runTest {
            val store =
                FakeConnectionSettingsStore(
                    ConnectionSettings(
                        customHeaders =
                            listOf(
                                ServerRequestHeader(name = "A", value = "1", id = "h1"),
                                ServerRequestHeader(name = "B", value = "2", id = "h2"),
                            ),
                        userAgent = "TestAgent/1.0",
                    ),
                )
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.saveHeader(ServerRequestHeader(name = "A", value = "updated", id = "h1"))
            advanceUntilIdle()

            assertEquals(
                listOf(
                    ServerRequestHeader(name = "A", value = "updated", id = "h1"),
                    ServerRequestHeader(name = "B", value = "2", id = "h2"),
                ),
                store.settings.first().customHeaders,
            )
        }

    @Test
    fun `removeHeader drops the header by id`() =
        runTest {
            val store =
                FakeConnectionSettingsStore(
                    ConnectionSettings(
                        customHeaders =
                            listOf(
                                ServerRequestHeader(name = "A", value = "1", id = "h1"),
                                ServerRequestHeader(name = "B", value = "2", id = "h2"),
                            ),
                        userAgent = "TestAgent/1.0",
                    ),
                )
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.removeHeader("h1")
            advanceUntilIdle()

            assertEquals(
                listOf(ServerRequestHeader(name = "B", value = "2", id = "h2")),
                store.settings.first().customHeaders,
            )
        }

    @Test
    fun `setBypassSsl persists the toggle`() =
        runTest {
            val store = FakeConnectionSettingsStore()
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.setBypassSsl(true)
            advanceUntilIdle()
            assertTrue(store.settings.first().bypassSsl)

            viewModel.setBypassSsl(false)
            advanceUntilIdle()
            assertFalse(store.settings.first().bypassSsl)
        }

    @Test
    fun `client certificate alias persists and clears with null`() =
        runTest {
            val store = FakeConnectionSettingsStore()
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.setClientCertificateAlias("alias-1")
            advanceUntilIdle()
            assertEquals("alias-1", store.settings.first().clientCertificateAlias)

            viewModel.setClientCertificateAlias(null)
            advanceUntilIdle()
            assertNull(store.settings.first().clientCertificateAlias)
        }

    @Test
    fun `setUserAgent and resetUserAgent persist`() =
        runTest {
            val store = FakeConnectionSettingsStore()
            val viewModel = ConnectionSettingsViewModel(store)

            viewModel.setUserAgent("CustomAgent/2.0")
            advanceUntilIdle()
            assertEquals("CustomAgent/2.0", store.settings.first().userAgent)

            viewModel.resetUserAgent()
            advanceUntilIdle()
            assertEquals(FakeConnectionSettingsStore.DEFAULT_USER_AGENT, store.settings.first().userAgent)
        }

    private class FakeConnectionSettingsStore(
        initial: ConnectionSettings = ConnectionSettings(userAgent = DEFAULT_USER_AGENT),
    ) : ConnectionSettingsStore {
        private val state = MutableStateFlow(initial)

        override val settings: Flow<ConnectionSettings> = state.asStateFlow()

        override suspend fun setCustomHeaders(headers: List<ServerRequestHeader>) {
            state.update { it.copy(customHeaders = headers) }
        }

        override suspend fun setBypassSsl(enabled: Boolean) {
            state.update { it.copy(bypassSsl = enabled) }
        }

        override suspend fun setClientCertificateAlias(alias: String?) {
            state.update { it.copy(clientCertificateAlias = alias) }
        }

        override suspend fun setUserAgent(userAgent: String) {
            state.update { it.copy(userAgent = userAgent) }
        }

        override suspend fun resetUserAgent() {
            state.update { it.copy(userAgent = DEFAULT_USER_AGENT) }
        }

        companion object {
            const val DEFAULT_USER_AGENT = "FakeAgent/1.0"
        }
    }
}
