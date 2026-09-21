package org.evoionosp.noveliq.data.connection

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings
import org.evoionosp.noveliq.domain.connection.model.ServerRequestHeader
import org.evoionosp.noveliq.domain.connection.model.sanitizeUserAgent
import org.evoionosp.noveliq.domain.connection.model.sanitizedForStorage

/**
 * Persists [ConnectionSettings]. Takes the backing [SharedPreferences] as a
 * constructor dependency so tests can supply a plain in-memory instance;
 * production passes the app-private file built by [ConnectionPreferences].
 * Headers may carry secrets (API keys), so the file is excluded from backup
 * (see backup_rules.xml).
 */
class ConnectionSettingsDataStore(
    private val preferences: SharedPreferences,
    private val gson: Gson = Gson(),
    private val defaultUserAgent: String = defaultUserAgent(),
) : ConnectionSettingsStore {
    private object Keys {
        const val CUSTOM_HEADERS = "custom_headers"
        const val BYPASS_SSL = "bypass_ssl"
        const val CLIENT_CERT_ALIAS = "client_cert_alias"
        const val USER_AGENT = "user_agent"
    }

    private val settingsState by lazy { MutableStateFlow(readSettings()) }

    override val settings: Flow<ConnectionSettings> = settingsState.asStateFlow()

    override suspend fun setCustomHeaders(headers: List<ServerRequestHeader>) {
        // Sanitize on the one path into storage so nothing downstream ever
        // sees an unclean row.
        val sanitized = headers.sanitizedForStorage()
        preferences.edit(commit = true) {
            putString(Keys.CUSTOM_HEADERS, gson.toJson(sanitized))
        }
        settingsState.value = settingsState.value.copy(customHeaders = sanitized)
    }

    override suspend fun setBypassSsl(enabled: Boolean) {
        preferences.edit(commit = true) {
            putBoolean(Keys.BYPASS_SSL, enabled)
        }
        settingsState.value = settingsState.value.copy(bypassSsl = enabled)
    }

    override suspend fun setClientCertificateAlias(alias: String?) {
        preferences.edit(commit = true) {
            if (alias.isNullOrBlank()) {
                remove(Keys.CLIENT_CERT_ALIAS)
            } else {
                putString(Keys.CLIENT_CERT_ALIAS, alias)
            }
        }
        settingsState.value = settingsState.value.copy(clientCertificateAlias = alias?.takeIf { it.isNotBlank() })
    }

    override suspend fun setUserAgent(userAgent: String) {
        val sanitized = sanitizeUserAgent(userAgent).ifBlank { defaultUserAgent }
        preferences.edit(commit = true) {
            putString(Keys.USER_AGENT, sanitized)
        }
        settingsState.value = settingsState.value.copy(userAgent = sanitized)
    }

    override suspend fun resetUserAgent() {
        setUserAgent(defaultUserAgent)
    }

    private fun readSettings(): ConnectionSettings =
        ConnectionSettings(
            customHeaders = readHeaders(),
            bypassSsl = preferences.getBoolean(Keys.BYPASS_SSL, false),
            clientCertificateAlias = preferences.getString(Keys.CLIENT_CERT_ALIAS, null),
            userAgent =
                sanitizeUserAgent(
                    preferences.getString(Keys.USER_AGENT, null).orEmpty(),
                ).ifBlank { defaultUserAgent },
        )

    private fun readHeaders(): List<ServerRequestHeader> {
        val json = preferences.getString(Keys.CUSTOM_HEADERS, null) ?: return emptyList()
        // A corrupt payload must not brick the settings screen: fall back to
        // no headers and let the next write repair the stored value.
        return runCatching {
            gson.fromJson(json, Array<ServerRequestHeader>::class.java).toList()
        }.getOrDefault(emptyList())
    }
}
