package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.session.SessionStore

class ClearSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    suspend operator fun invoke() {
        sessionStore.clearSession()
    }
}
