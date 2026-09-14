package org.evoionosp.noveliq.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UrlUtilsTest {
    @Test
    fun `blank base urls are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { UrlUtils.normalizeBaseUrl("") }
        assertThrows(IllegalArgumentException::class.java) { UrlUtils.normalizeBaseUrl("   ") }
    }

    @Test
    fun `schemeless hosts default to https`() {
        assertEquals("https://example.com/", UrlUtils.normalizeBaseUrl("example.com"))
        assertEquals("https://example.com/", UrlUtils.normalizeBaseUrl("  example.com  "))
    }

    @Test
    fun `explicit schemes and trailing slashes are preserved`() {
        assertEquals("http://example.com/", UrlUtils.normalizeBaseUrl("http://example.com"))
        assertEquals("https://example.com/", UrlUtils.normalizeBaseUrl("https://example.com/"))
        assertEquals("https://example.com/a/", UrlUtils.normalizeBaseUrl("https://example.com/a"))
    }
}
