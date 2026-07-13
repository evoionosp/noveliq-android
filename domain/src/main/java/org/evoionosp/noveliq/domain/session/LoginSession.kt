package org.evoionosp.noveliq.domain.session

data class LoginSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String?,
    val username: String,
    val baseUrl: String
) {
    fun isValid(): Boolean {
        return accessToken.isNotBlank() && username.isNotBlank() && baseUrl.isNotBlank()
    }
}
