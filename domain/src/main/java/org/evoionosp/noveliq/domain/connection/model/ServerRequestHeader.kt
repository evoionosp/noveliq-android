package org.evoionosp.noveliq.domain.connection.model

import java.util.UUID

/**
 * One user-defined HTTP header attached to every server request. Header
 * names keep lissen-android's RFC 7230 token filter (MIT,
 * GrakovNe/lissen-android); values deliberately only strip control
 * characters so legal values like "Bearer <token>" survive, where lissen
 * applies the stricter token filter to values too.
 */
data class ServerRequestHeader(
    val name: String,
    val value: String,
    val id: String = UUID.randomUUID().toString(),
) {
    /**
     * Returns this header with its name and value sanitized for storage.
     * Names drop every character outside the RFC 7230 token set (OkHttp
     * rejects anything else); values drop control characters only.
     */
    fun clean(): ServerRequestHeader =
        copy(
            name = name.cleanHeaderName(),
            value = value.cleanHeaderValue(),
        )

    private fun String.cleanHeaderName(): String = replace(HEADER_NAME_INVALID_CHARS, "").trim()

    private fun String.cleanHeaderValue(): String = replace(HEADER_VALUE_INVALID_CHARS, "").trim()

    private companion object {
        val HEADER_NAME_INVALID_CHARS = Regex("[^!#\$%&'*+\\-.^_`|~0-9A-Za-z]")
        val HEADER_VALUE_INVALID_CHARS = Regex("[\\x00-\\x1F\\x7F]")
    }
}

/**
 * Sanitizes raw editor rows for storage: cleans each row, keeps the first
 * row per header name, and drops rows with an empty name or value.
 */
fun List<ServerRequestHeader>.sanitizedForStorage(): List<ServerRequestHeader> =
    map { it.clean() }
        .distinctBy { it.name }
        .filterNot { it.name.isEmpty() }
        .filterNot { it.value.isEmpty() }

/**
 * Strips control characters from a raw User-Agent for storage. Blank stays
 * blank; the repository falls back to the default agent for blank input.
 */
fun sanitizeUserAgent(raw: String): String = raw.replace(USER_AGENT_INVALID_CHARS, "").trim()

private val USER_AGENT_INVALID_CHARS = Regex("[\\x00-\\x1F\\x7F]")
