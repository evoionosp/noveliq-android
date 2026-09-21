package org.evoionosp.noveliq.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.evoionosp.noveliq.domain.settings.AppSettings
import org.evoionosp.noveliq.domain.settings.AppSettingsStore

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(
    private val context: Context,
) : AppSettingsStore {
    private object Keys {
        val themePreference = stringPreferencesKey("theme_preference")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val useCoverTheme = booleanPreferencesKey("use_cover_theme")
        val sleepTimerMinutes = intPreferencesKey("sleep_timer_minutes")
    }

    override val settings: Flow<AppSettings> =
        context.appSettingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map(::toAppSettings)

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

    override suspend fun setCoverTheme(enabled: Boolean) {
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.useCoverTheme] = enabled
        }
    }

    override suspend fun setSleepTimerMinutes(minutes: Int) {
        // Coerce on the one path into storage so a stale or corrupt write
        // can never persist a value outside the slider range.
        val coerced =
            minutes.coerceIn(
                AppSettings.MIN_SLEEP_TIMER_MINUTES,
                AppSettings.MAX_SLEEP_TIMER_MINUTES,
            )
        context.appSettingsDataStore.edit { preferences ->
            preferences[Keys.sleepTimerMinutes] = coerced
        }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings =
        AppSettings(
            themePreference =
                preferences[Keys.themePreference]
                    ?: AppSettings.DEFAULT_THEME_PREFERENCE,
            useDynamicColor = preferences[Keys.useDynamicColor] ?: true,
            useCoverTheme = preferences[Keys.useCoverTheme] ?: true,
            sleepTimerMinutes =
                (preferences[Keys.sleepTimerMinutes] ?: AppSettings.DEFAULT_SLEEP_TIMER_MINUTES)
                    .coerceIn(
                        AppSettings.MIN_SLEEP_TIMER_MINUTES,
                        AppSettings.MAX_SLEEP_TIMER_MINUTES,
                    ),
        )
}
