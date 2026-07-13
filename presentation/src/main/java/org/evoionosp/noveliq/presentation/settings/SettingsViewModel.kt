package org.evoionosp.noveliq.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evoionosp.noveliq.domain.settings.AppSettingsStore
import org.evoionosp.noveliq.presentation.theme.ThemePreference

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsStore: AppSettingsStore
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = appSettingsStore.settings
        .map { settings ->
            SettingsUiState(
                themePreference = ThemePreference.entries.firstOrNull {
                    it.name == settings.themePreference
                } ?: ThemePreference.SYSTEM,
                useDynamicColor = settings.useDynamicColor
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun onThemePreferenceChange(preference: ThemePreference) {
        viewModelScope.launch {
            appSettingsStore.setThemePreference(preference.name)
        }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsStore.setDynamicColor(enabled)
        }
    }
}
