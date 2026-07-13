package org.evoionosp.noveliq.domain.auth.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

/**
 * Refreshes the access token for the current session using its stored refresh token, persists the
 * rotated session, and returns it. Returns null when there is no session, no usable refresh token,
 * or the refresh request fails — callers treat null as "session expired".
 */
class RefreshSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): LoginSession? {
        val current = sessionStore.session.first() ?: return null
        val refreshToken = current.refreshToken?.takeIf { it.isNotBlank() } ?: return null
        return when (
            val result = authRepository.refreshSession(
                baseUrl = current.baseUrl,
                refreshToken = refreshToken
            )
        ) {
            is LoginResult.Success -> {
                val accessToken = result.data.accessToken?.trim().orEmpty()
                if (accessToken.isBlank()) return null
                val updatedSession = current.copy(
                    accessToken = accessToken,
                    refreshToken = result.data.refreshToken?.trim()?.takeIf { it.isNotBlank() }
                        ?: current.refreshToken,
                    userId = result.data.userId?.trim() ?: current.userId
                )
                sessionStore.saveSession(updatedSession)
                updatedSession
            }
            is LoginResult.Failure -> null
        }
    }
}
