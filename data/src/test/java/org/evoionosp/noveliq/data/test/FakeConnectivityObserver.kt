package org.evoionosp.noveliq.data.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.evoionosp.noveliq.domain.connectivity.ConnectivityObserver

class FakeConnectivityObserver(
    connected: Boolean = true,
) : ConnectivityObserver {
    private val state = MutableStateFlow(connected)

    var connected: Boolean = connected
        private set

    override fun observe(): Flow<Boolean> = state

    override fun isConnected(): Boolean = connected

    fun setConnected(value: Boolean) {
        connected = value
        state.value = value
    }
}
