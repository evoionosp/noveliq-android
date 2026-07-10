package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

class ObserveSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    operator fun invoke(): Flow<LoginSession?> {
        return sessionStore.session
    }
}
