package org.evoionosp.noveliq.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.navigation.NoveliqApp
import org.evoionosp.noveliq.presentation.settings.SettingsViewModel
import org.evoionosp.noveliq.presentation.splash.SplashViewModel
import org.evoionosp.noveliq.presentation.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.uiState.value.isLoading
        }
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

            AppTheme(
                themePreference = settingsState.themePreference,
                dynamicColor = settingsState.useDynamicColor
            ) {
                NoveliqApp(
                    splashState = splashState,
                    settingsState = settingsState,
                    onRetryCatalogBootstrap = splashViewModel::retryCatalogBootstrap,
                    onLogout = splashViewModel::logout,
                    onThemePreferenceChange = settingsViewModel::onThemePreferenceChange,
                    onDynamicColorChange = settingsViewModel::onDynamicColorChange
                )
            }
        }
    }
}
