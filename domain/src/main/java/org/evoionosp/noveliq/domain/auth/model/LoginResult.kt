package org.evoionosp.noveliq.domain.auth.model

sealed interface LoginResult {
    data class Success(val data: LoginData) : LoginResult
    data class Failure(val message: String, val code: Int? = null) : LoginResult
}
