package org.evoionosp.noveliq.domain.auth.usecase

import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {
        return repository.login(
            baseUrl = baseUrl,
            username = username,
            password = password
        )
    }
}
