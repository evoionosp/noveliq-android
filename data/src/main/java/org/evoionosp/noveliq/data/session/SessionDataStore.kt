package org.evoionosp.noveliq.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.evoionosp.noveliq.data.auth.JwtExpiry
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

class SessionDataStore(
    private val context: Context
) : SessionStore {
    private object Keys {
        const val accessToken = "access_token"
        const val refreshToken = "refresh_token"
        const val accessTokenExpiresAt = "access_token_expires_at"
        const val userId = "user_id"
        const val username = "username"
        const val baseUrl = "base_url"
        const val lastServerUrl = "last_server_url"
    }

    /** Everything that belongs to the signed-in session, and so is dropped on logout. */
    private val sessionKeys = listOf(
        Keys.accessToken,
        Keys.refreshToken,
        Keys.accessTokenExpiresAt,
        Keys.userId,
        Keys.username,
        Keys.baseUrl
    )

    private val encryptedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "encrypted_session_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val sessionState by lazy { MutableStateFlow(readEncryptedSession()) }
    private val lastServerUrlState by lazy { MutableStateFlow(readLastServerUrl()) }

    override val session: Flow<LoginSession?> = sessionState.asStateFlow()

    override val lastServerUrl: Flow<String?> = lastServerUrlState.asStateFlow()

    override suspend fun saveSession(session: LoginSession): LoginSession {
        // Derive the expiry here rather than trusting callers: this is the one path every token
        // takes into storage, whether it came from a login or a refresh, so nothing can slip
        // through without it.
        val stored = session.copy(
            accessTokenExpiresAtEpochSeconds = JwtExpiry.expiresAtEpochSeconds(session.accessToken)
        )

        encryptedPreferences.edit(commit = true) {
            putString(Keys.accessToken, stored.accessToken)
            putString(Keys.username, stored.username)
            putString(Keys.baseUrl, stored.baseUrl)
            putOptional(Keys.refreshToken, stored.refreshToken)
            putOptional(Keys.userId, stored.userId)
            putOptional(Keys.accessTokenExpiresAt, stored.accessTokenExpiresAtEpochSeconds)
            putOptional(Keys.lastServerUrl, stored.baseUrl)
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
        val accessToken = encryptedPreferences.getString(Keys.accessToken, null).orEmpty()
        val session = LoginSession(
            accessToken = accessToken,
            refreshToken = encryptedPreferences.getString(Keys.refreshToken, null),
            userId = encryptedPreferences.getString(Keys.userId, null),
            username = encryptedPreferences.getString(Keys.username, null).orEmpty(),
            baseUrl = encryptedPreferences.getString(Keys.baseUrl, null).orEmpty(),
            // Sessions persisted before expiry tracking existed have no stored value, so fall
            // back to reading it off the token. That way an install that has been sitting on a
            // long-dead token picks up proactive refresh without needing to log in again first.
            accessTokenExpiresAtEpochSeconds = readExpiry()
                ?: accessToken.takeIf { it.isNotBlank() }
                    ?.let { JwtExpiry.expiresAtEpochSeconds(it) }
        )
        return session.takeIf { it.isValid() }
    }

    private fun readLastServerUrl(): String? {
        return encryptedPreferences.getString(Keys.lastServerUrl, null)
            // Installs that signed in before the server URL was remembered separately still have
            // the session's base URL, so seed from that.
            ?: encryptedPreferences.getString(Keys.baseUrl, null)
    }

    private fun readExpiry(): Long? {
        return encryptedPreferences.getLong(Keys.accessTokenExpiresAt, NO_EXPIRY)
            .takeIf { it != NO_EXPIRY }
    }

    private fun SharedPreferences.Editor.putOptional(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value)
        }
    }

    private fun SharedPreferences.Editor.putOptional(key: String, value: Long?) {
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
