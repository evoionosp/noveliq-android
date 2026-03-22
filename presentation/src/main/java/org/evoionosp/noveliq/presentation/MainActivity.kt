package org.evoionosp.noveliq.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.auth.AuthScreen
import org.evoionosp.noveliq.presentation.settings.SettingsViewModel
import org.evoionosp.noveliq.presentation.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            AppTheme(
                themePreference = settingsState.themePreference,
                dynamicColor = settingsState.useDynamicColor
            ) {
                AuthScreen(
                    modifier = Modifier,
                    settingsState = settingsState,
                    onThemePreferenceChange = settingsViewModel::onThemePreferenceChange,
                    onDynamicColorChange = settingsViewModel::onDynamicColorChange
                )
            }
        }
    }
}
