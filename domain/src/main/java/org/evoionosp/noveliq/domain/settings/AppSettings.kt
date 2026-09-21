package org.evoionosp.noveliq.domain.settings

data class AppSettings(
    val themePreference: String = DEFAULT_THEME_PREFERENCE,
    val useDynamicColor: Boolean = true,
    val useCoverTheme: Boolean = true,
    val sleepTimerMinutes: Int = DEFAULT_SLEEP_TIMER_MINUTES,
) {
    companion object {
        const val DEFAULT_THEME_PREFERENCE = "SYSTEM"

        /** Last sleep-timer duration picked on the slider or presets. */
        const val DEFAULT_SLEEP_TIMER_MINUTES = 30
        const val MIN_SLEEP_TIMER_MINUTES = 1
        const val MAX_SLEEP_TIMER_MINUTES = 100
    }
}
