package org.evoionosp.noveliq.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.evoionosp.noveliq.presentation.ui.theme.ThemePreference

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onThemePreferenceChange(preference: ThemePreference) {
        _uiState.update { it.copy(themePreference = preference) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        _uiState.update { it.copy(useDynamicColor = enabled) }
    }
}
