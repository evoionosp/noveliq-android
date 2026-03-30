package org.evoionosp.noveliq.domain.auth.repository

import org.evoionosp.noveliq.domain.auth.model.LoginResult

interface AuthRepository {
    suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult
}