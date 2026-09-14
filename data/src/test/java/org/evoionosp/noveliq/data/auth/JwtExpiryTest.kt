package org.evoionosp.noveliq.data.auth

import java.util.Base64 as JdkBase64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JwtExpiryTest {
    @Test
    fun `valid tokens expose expiry`() {
        assertEquals(
            1_700_000_000L,
            JwtExpiry.expiresAtEpochSeconds(token(payload = """{"exp":1700000000}""")),
        )
    }

    @Test
    fun `tokens without expiry return null`() {
        assertNull(JwtExpiry.expiresAtEpochSeconds(token(payload = """{"sub":"user"}""")))
    }

    @Test
    fun `malformed tokens return null`() {
        assertNull(JwtExpiry.expiresAtEpochSeconds("not-a-jwt"))
        assertNull(JwtExpiry.expiresAtEpochSeconds("a.!!!.c"))
        assertNull(JwtExpiry.expiresAtEpochSeconds(token(payload = "not json")))
    }

    private fun token(payload: String): String {
        val encoded =
            JdkBase64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "header.$encoded.signature"
    }
}
