package org.evoionosp.noveliq.data.network

object UrlUtils {
    fun normalizeBaseUrl(baseUrl: String): String {
        var trimmed = baseUrl.trim()
        require(trimmed.isNotEmpty()) { "Base URL must not be blank." }

        // If no scheme is provided, default to https://
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed = "https://$trimmed"
        }

        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
