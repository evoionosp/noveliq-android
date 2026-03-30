package org.evoionosp.noveliq.presentation.settings

import org.evoionosp.noveliq.presentation.ui.theme.ThemePreference

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val useDynamicColor: Boolean = true
)
