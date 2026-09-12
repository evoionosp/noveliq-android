package org.evoionosp.noveliq.data.auth

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads the `exp` claim out of a JWT access token.
 *
 * Audiobookshelf 2.26.0 and later issue short-lived JWT access tokens alongside a longer-lived
 * refresh token. Knowing when the access token lapses lets the app rotate it before a call fails
 * rather than paying for a 401 round trip first — which matters most on a cold start after the
 * app has been closed for longer than the token's lifetime.
 *
 * Tokens that are not JWTs (older servers issue opaque tokens) simply report no expiry, and the
 * app falls back to reacting to 401s.
 */
internal object JwtExpiry {
    private const val JWT_PART_COUNT = 3
    private const val PAYLOAD_INDEX = 1
    private const val EXPIRY_CLAIM = "exp"
    private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    /** Returns the token's expiry as a Unix epoch second, or null when it cannot be determined. */
    fun expiresAtEpochSeconds(token: String): Long? {
        val parts = token.split('.')
        if (parts.size != JWT_PART_COUNT) return null

        return try {
            val payload = Base64.decode(parts[PAYLOAD_INDEX], BASE64_FLAGS)
            val claims = JSONObject(String(payload, Charsets.UTF_8))
            if (claims.has(EXPIRY_CLAIM)) claims.getLong(EXPIRY_CLAIM) else null
        } catch (_: IllegalArgumentException) {
            // Payload was not valid base64url.
            null
        } catch (_: JSONException) {
            // Payload decoded but was not a JSON object, or `exp` was not a number.
            null
        }
    }
}
