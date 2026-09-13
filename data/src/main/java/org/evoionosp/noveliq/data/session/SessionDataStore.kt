package org.evoionosp.noveliq.data.session

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.evoionosp.noveliq.data.auth.JwtExpiry
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

/**
 * Persists the login session. Takes the backing [SharedPreferences] as a constructor
 * dependency so tests can supply a plain in-memory instance; production passes the
 * encrypted file built by [EncryptedSessionPreferences].
 */
class SessionDataStore(
    private val encryptedPreferences: SharedPreferences,
) : SessionStore {
    private object Keys {
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val BASE_URL = "base_url"
        const val LAST_SERVER_URL = "last_server_url"
    }

    /** Everything that belongs to the signed-in session, and so is dropped on logout. */
    private val sessionKeys =
        listOf(
            Keys.ACCESS_TOKEN,
            Keys.REFRESH_TOKEN,
            Keys.ACCESS_TOKEN_EXPIRES_AT,
            Keys.USER_ID,
            Keys.USERNAME,
            Keys.BASE_URL,
        )

    private val sessionState by lazy { MutableStateFlow(readEncryptedSession()) }
    private val lastServerUrlState by lazy { MutableStateFlow(readLastServerUrl()) }

    override val session: Flow<LoginSession?> = sessionState.asStateFlow()

    override val lastServerUrl: Flow<String?> = lastServerUrlState.asStateFlow()

    override suspend fun saveSession(session: LoginSession): LoginSession {
        // Derive the expiry here rather than trusting callers: this is the one path every token
        // takes into storage, whether it came from a login or a refresh, so nothing can slip
        // through without it.
        val stored =
            session.copy(
                accessTokenExpiresAtEpochSeconds =
                    JwtExpiry.expiresAtEpochSeconds(
                        session.accessToken,
                    ),
            )

        encryptedPreferences.edit(commit = true) {
            putString(Keys.ACCESS_TOKEN, stored.accessToken)
            putString(Keys.USERNAME, stored.username)
            putString(Keys.BASE_URL, stored.baseUrl)
            putOptional(Keys.REFRESH_TOKEN, stored.refreshToken)
            putOptional(Keys.USER_ID, stored.userId)
            putOptional(Keys.ACCESS_TOKEN_EXPIRES_AT, stored.accessTokenExpiresAtEpochSeconds)
            putOptional(Keys.LAST_SERVER_URL, stored.baseUrl)
        }
        sessionState.value = stored
        lastServerUrlState.value = stored.baseUrl.takeIf { it.isNotBlank() }
        return stored
    }

    override suspend fun clearSession() {
        // Remove the session keys individually rather than clearing the file, so the remembered
        // server URL survives a logout and can prefill the login form.
        encryptedPreferences.edit(commit = true) {
            sessionKeys.forEach(::remove)
        }
        sessionState.value = null
    }

    private fun readEncryptedSession(): LoginSession? {
        val accessToken = encryptedPreferences.getString(Keys.ACCESS_TOKEN, null).orEmpty()
        val session =
            LoginSession(
                accessToken = accessToken,
                refreshToken = encryptedPreferences.getString(Keys.REFRESH_TOKEN, null),
                userId = encryptedPreferences.getString(Keys.USER_ID, null),
                username = encryptedPreferences.getString(Keys.USERNAME, null).orEmpty(),
                baseUrl = encryptedPreferences.getString(Keys.BASE_URL, null).orEmpty(),
                // Sessions persisted before expiry tracking existed have no stored value, so fall
                // back to reading it off the token. That way an install that has been sitting on a
                // long-dead token picks up proactive refresh without needing to log in again first.
                accessTokenExpiresAtEpochSeconds =
                    readExpiry()
                        ?: accessToken
                            .takeIf { it.isNotBlank() }
                            ?.let { JwtExpiry.expiresAtEpochSeconds(it) },
            )
        return session.takeIf { it.isValid() }
    }

    private fun readLastServerUrl(): String? =
        encryptedPreferences.getString(Keys.LAST_SERVER_URL, null)
            // Installs that signed in before the server URL was remembered separately still have
            // the session's base URL, so seed from that.
            ?: encryptedPreferences.getString(Keys.BASE_URL, null)

    private fun readExpiry(): Long? =
        encryptedPreferences
            .getLong(Keys.ACCESS_TOKEN_EXPIRES_AT, NO_EXPIRY)
            .takeIf { it != NO_EXPIRY }

    private fun SharedPreferences.Editor.putOptional(
        key: String,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value)
        }
    }

    private fun SharedPreferences.Editor.putOptional(
        key: String,
        value: Long?,
    ) {
        if (value == null) {
            remove(key)
        } else {
            putLong(key, value)
        }
    }

    private companion object {
        const val NO_EXPIRY = -1L
    }
}
