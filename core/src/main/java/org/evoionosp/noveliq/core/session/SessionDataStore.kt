package org.evoionosp.noveliq.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionDataStore(
    private val context: Context
) : SessionStore {
    private object Keys {
        const val accessToken = "access_token"
        const val refreshToken = "refresh_token"
        const val userId = "user_id"
        const val username = "username"
        const val baseUrl = "base_url"
    }

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

    override val session: Flow<LoginSession?> = sessionState.asStateFlow()

    override suspend fun saveSession(session: LoginSession) {
        encryptedPreferences.edit(commit = true) {
            putString(Keys.accessToken, session.accessToken)
            putString(Keys.username, session.username)
            putString(Keys.baseUrl, session.baseUrl)
            putOptional(Keys.refreshToken, session.refreshToken)
            putOptional(Keys.userId, session.userId)
        }
        sessionState.value = session
    }

    override suspend fun clearSession() {
        encryptedPreferences.edit(commit = true) { clear() }
        sessionState.value = null
    }

    private fun readEncryptedSession(): LoginSession? {
        val session = LoginSession(
            accessToken = encryptedPreferences.getString(Keys.accessToken, null).orEmpty(),
            refreshToken = encryptedPreferences.getString(Keys.refreshToken, null),
            userId = encryptedPreferences.getString(Keys.userId, null),
            username = encryptedPreferences.getString(Keys.username, null).orEmpty(),
            baseUrl = encryptedPreferences.getString(Keys.baseUrl, null).orEmpty()
        )
        return session.takeIf { it.isValid() }
    }

    private fun SharedPreferences.Editor.putOptional(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value)
        }
    }
}
