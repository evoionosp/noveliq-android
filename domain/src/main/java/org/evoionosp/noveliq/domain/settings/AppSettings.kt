package org.evoionosp.noveliq.domain.settings

data class AppSettings(
    val themePreference: String = DEFAULT_THEME_PREFERENCE,
    val useDynamicColor: Boolean = true,
    val useCoverTheme: Boolean = true,
) {
    companion object {
        const val DEFAULT_THEME_PREFERENCE = "SYSTEM"
    }
}
