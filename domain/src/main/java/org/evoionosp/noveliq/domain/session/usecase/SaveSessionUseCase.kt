package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

class SaveSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    suspend operator fun invoke(session: LoginSession) {
        sessionStore.saveSession(session)
    }
}
