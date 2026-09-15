package org.evoionosp.noveliq.domain.session

/**
 * Stops playback as part of logout. Runs before the session is cleared so any
 * final progress flush still has a valid token. Implemented in `:playback`.
 */
fun interface PlayerLogoutHandler {
    suspend fun onLogout()
}
