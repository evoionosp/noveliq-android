package org.evoionosp.noveliq.domain.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Boolean>

    fun isConnected(): Boolean
}
