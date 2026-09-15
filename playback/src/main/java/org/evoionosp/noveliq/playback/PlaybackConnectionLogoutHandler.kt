package org.evoionosp.noveliq.playback

import javax.inject.Inject
import javax.inject.Singleton
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler

/**
 * Logout's playback step. Awaits the final progress flush while the session is
 * still valid, then stops the player and resets its state.
 */
@Singleton
class PlaybackConnectionLogoutHandler
    @Inject
    constructor(
        private val playbackConnection: PlaybackConnection,
    ) : PlayerLogoutHandler {
        override suspend fun onLogout() {
            playbackConnection.stopForLogout()
        }
    }
