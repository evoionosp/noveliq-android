package org.evoionosp.noveliq.presentation.common

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for providing the current access token to composables
 * that need to load authenticated resources (e.g., cover images).
 *
 * This eliminates prop drilling of accessToken through multiple layers.
 */
val LocalAccessToken = compositionLocalOf<String> {
    error("No access token provided")
}
