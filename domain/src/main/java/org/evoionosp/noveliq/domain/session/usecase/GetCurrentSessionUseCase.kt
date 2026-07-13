package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

class GetCurrentSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore
) {
    suspend operator fun invoke(): LoginSession? {
        return sessionStore.session.first()
    }
}
