package org.evoionosp.noveliq.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.evoionosp.noveliq.presentation.server.ServerSetupScreen
import org.evoionosp.noveliq.presentation.ui.theme.NoveliqTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoveliqTheme {
                ServerSetupScreen(modifier = Modifier)
            }
        }
    }
}
