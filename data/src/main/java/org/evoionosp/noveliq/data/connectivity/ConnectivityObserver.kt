package org.evoionosp.noveliq.data.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Boolean>

    fun isConnected(): Boolean
}
