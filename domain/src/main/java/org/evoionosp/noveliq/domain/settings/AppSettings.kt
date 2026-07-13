package org.evoionosp.noveliq.domain.settings

data class AppSettings(
    val themePreference: String = DEFAULT_THEME_PREFERENCE,
    val useDynamicColor: Boolean = true
) {
    companion object {
        const val DEFAULT_THEME_PREFERENCE = "SYSTEM"
    }
}
