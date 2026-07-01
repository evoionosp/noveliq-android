package org.evoionosp.noveliq.core.session

import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val session: Flow<LoginSession?>

    suspend fun saveSession(session: LoginSession)

    suspend fun clearSession()
}
