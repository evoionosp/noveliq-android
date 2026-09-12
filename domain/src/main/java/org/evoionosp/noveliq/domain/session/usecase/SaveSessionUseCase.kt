package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

class SaveSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    /** Persists [session] and returns it as stored, including any values the store derived. */
    suspend operator fun invoke(session: LoginSession): LoginSession {
        return sessionStore.saveSession(session)
    }
}
