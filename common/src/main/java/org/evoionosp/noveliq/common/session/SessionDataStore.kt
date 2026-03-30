package org.evoionosp.noveliq.common.session

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session_store")

class SessionDataStore(
    private val context: Context
) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val username = stringPreferencesKey("username")
        val baseUrl = stringPreferencesKey("base_url")
    }

    val session: Flow<LoginSession?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val accessToken = preferences[Keys.accessToken].orEmpty()
            val username = preferences[Keys.username].orEmpty()
            val baseUrl = preferences[Keys.baseUrl].orEmpty()
            val session = LoginSession(
                accessToken = accessToken,
                refreshToken = preferences[Keys.refreshToken],
                userId = preferences[Keys.userId],
                username = username,
                baseUrl = baseUrl
            )
            if (session.isValid()) session else null
        }

    suspend fun saveSession(session: LoginSession) {
        context.sessionDataStore.edit { preferences ->
            preferences[Keys.accessToken] = session.accessToken
            preferences[Keys.username] = session.username
            preferences[Keys.baseUrl] = session.baseUrl
            setOptional(preferences, Keys.refreshToken, session.refreshToken)
            setOptional(preferences, Keys.userId, session.userId)
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }

    private fun setOptional(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?
    ) {
        if (value.isNullOrBlank()) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }
}
