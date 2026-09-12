package org.evoionosp.noveliq.domain.session

data class LoginSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String?,
    val username: String,
    val baseUrl: String,
    /**
     * Unix epoch second at which [accessToken] stops being accepted, when that is known.
     *
     * Null when the app could not read an expiry off the token: opaque tokens from older
     * Audiobookshelf servers, and sessions persisted before expiry tracking existed. Those fall
     * back to reactive handling — the request 401s and the token is refreshed then.
     */
    val accessTokenExpiresAtEpochSeconds: Long? = null
) {
    /**
     * Structural validity only: whether this session carries the fields needed to build a
     * request. Deliberately says nothing about expiry — an expired access token still belongs to
     * a recoverable session as long as the refresh token holds, so discarding it here would log
     * the user out of a session that only needed refreshing. Expiry is [isAccessTokenExpired].
     */
    fun isValid(): Boolean {
        return accessToken.isNotBlank() && username.isNotBlank() && baseUrl.isNotBlank()
    }

    /**
     * True when [accessToken] is known to have expired, or is close enough to expiry that it
     * could lapse while a request is in flight. An unknown expiry reports false.
     */
    fun isAccessTokenExpired(
        nowEpochSeconds: Long,
        skewSeconds: Long = DEFAULT_EXPIRY_SKEW_SECONDS
    ): Boolean {
        val expiresAt = accessTokenExpiresAtEpochSeconds ?: return false
        return nowEpochSeconds >= expiresAt - skewSeconds
    }

    companion object {
        const val DEFAULT_EXPIRY_SKEW_SECONDS = 60L
    }
}
