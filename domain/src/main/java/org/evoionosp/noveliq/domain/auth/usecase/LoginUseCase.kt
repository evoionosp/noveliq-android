package org.evoionosp.noveliq.domain.auth.usecase

import javax.inject.Inject
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository

class LoginUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(
            baseUrl: String,
            username: String,
            password: String,
        ): LoginResult =
            repository.login(
                baseUrl = baseUrl,
                username = username,
                password = password,
            )
    }
