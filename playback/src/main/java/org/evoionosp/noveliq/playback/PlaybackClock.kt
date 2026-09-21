package org.evoionosp.noveliq.playback

/**
 * Wall clock for sleep-timer math. Injected (rather than calling
 * [System.currentTimeMillis] directly) so tests can drive the countdown by
 * hand; production binds the real clock in [PlaybackConnectionBindings].
 */
fun interface PlaybackClock {
    fun nowMs(): Long
}
