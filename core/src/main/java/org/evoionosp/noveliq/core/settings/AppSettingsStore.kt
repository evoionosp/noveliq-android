package org.evoionosp.noveliq.core.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemePreference(themePreference: String)

    suspend fun setDynamicColor(enabled: Boolean)
}
