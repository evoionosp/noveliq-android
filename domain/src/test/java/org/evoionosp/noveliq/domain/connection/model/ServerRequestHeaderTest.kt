package org.evoionosp.noveliq.domain.connection.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerRequestHeaderTest {
    @Test
    fun `clean strips non-token characters from names`() {
        val cleaned = ServerRequestHeader("X-Api Key/1", "v").clean()

        assertEquals("X-ApiKey1", cleaned.name)
    }

    @Test
    fun `clean trims names and values`() {
        val cleaned = ServerRequestHeader("  X-Key  ", "  value  ").clean()

        assertEquals("X-Key", cleaned.name)
        assertEquals("value", cleaned.value)
    }

    @Test
    fun `clean preserves spaces inside values`() {
        val cleaned = ServerRequestHeader("Authorization", "Bearer abc 123").clean()

        assertEquals("Bearer abc 123", cleaned.value)
    }

    @Test
    fun `clean strips control characters from values`() {
        val cleaned = ServerRequestHeader("X-Key", "a\nb\rc\u0000d\u007fe").clean()

        assertEquals("abcde", cleaned.value)
    }

    @Test
    fun `sanitizedForStorage keeps first row per name`() {
        val rows =
            listOf(
                ServerRequestHeader("X-Key", "first"),
                ServerRequestHeader("X-Key", "second"),
                ServerRequestHeader("Other", "v"),
            )

        val sanitized = rows.sanitizedForStorage()

        assertEquals(
            listOf("X-Key" to "first", "Other" to "v"),
            sanitized.map { it.name to it.value },
        )
    }

    @Test
    fun `sanitizedForStorage drops empty rows after cleaning`() {
        val rows =
            listOf(
                ServerRequestHeader("   ", "v"),
                ServerRequestHeader("X-Key", "  "),
                ServerRequestHeader("()", "v"),
                ServerRequestHeader("X-Ok", "v"),
            )

        val sanitized = rows.sanitizedForStorage()

        assertEquals(1, sanitized.size)
        assertEquals("X-Ok", sanitized[0].name)
    }

    @Test
    fun `sanitizeUserAgent strips control characters and trims`() {
        assertEquals("Mozilla/5.0", sanitizeUserAgent("  Mozilla/5.0\n"))
        assertEquals("", sanitizeUserAgent("  \r\n "))
    }
}
