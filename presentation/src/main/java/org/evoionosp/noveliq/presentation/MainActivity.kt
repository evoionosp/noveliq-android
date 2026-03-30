package org.evoionosp.noveliq.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.auth.AuthScreen
import org.evoionosp.noveliq.presentation.home.HomeScreen
import org.evoionosp.noveliq.presentation.settings.SettingsViewModel
import org.evoionosp.noveliq.presentation.splash.SplashScreen
import org.evoionosp.noveliq.presentation.splash.SplashViewModel
import org.evoionosp.noveliq.presentation.ui.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.uiState.value.isLoading
        }
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

            AppTheme(
                themePreference = settingsState.themePreference,
                dynamicColor = settingsState.useDynamicColor
            ) {
                when {
                    splashState.isLoading -> SplashScreen(modifier = Modifier)
                    splashState.session != null && splashState.isCatalogReady -> HomeScreen(
                        username = splashState.session!!.username,
                        accessToken = splashState.session!!.accessToken,
                        modifier = Modifier
                    )
                    else -> AuthScreen(
                        modifier = Modifier,
                        settingsState = settingsState,
                        onThemePreferenceChange = settingsViewModel::onThemePreferenceChange,
                        onDynamicColorChange = settingsViewModel::onDynamicColorChange
                    )
                }
            }
        }
    }
}
