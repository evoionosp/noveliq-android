package org.evoionosp.noveliq.core.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(
    private val context: Context
) : AppSettingsStore {
    private object Keys {
        val themePreference = stringPreferencesKey("theme_preference")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
    }

    override val settings: Flow<AppSettings> = context.appSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::toAppSettings)

    override suspend fun setThemePreference(themePreference: String) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.themePreference] = themePreference
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useDynamicColor] = enabled
        }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings {
        return AppSettings(
            themePreference = preferences[Keys.themePreference]
                ?: AppSettings.DEFAULT_THEME_PREFERENCE,
            useDynamicColor = preferences[Keys.useDynamicColor] ?: true
        )
    }
}
