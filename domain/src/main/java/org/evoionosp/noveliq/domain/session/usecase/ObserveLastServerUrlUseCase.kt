package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.evoionosp.noveliq.domain.session.SessionStore

/**
 * Emits the base URL of the server last signed in to, so the login form can offer it again after
 * a logout or an expired session. Emits null when the app has never had a session.
 */
class ObserveLastServerUrlUseCase
    @Inject
    constructor(
        private val sessionStore: SessionStore,
    ) {
        operator fun invoke(): Flow<String?> = sessionStore.lastServerUrl
    }
