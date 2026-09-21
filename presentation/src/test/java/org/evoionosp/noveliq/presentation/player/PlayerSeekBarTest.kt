package org.evoionosp.noveliq.presentation.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSeekBarTest {
    @Test
    fun `progress fraction is position over duration`() {
        assertEquals(0.5f, progressFraction(30_000L, 60_000L), 0.001f)
        assertEquals(0f, progressFraction(0L, 60_000L), 0.001f)
        assertEquals(1f, progressFraction(60_000L, 60_000L), 0.001f)
    }

    @Test
    fun `progress fraction is zero for unknown durations`() {
        assertEquals(0f, progressFraction(30_000L, 0L), 0.001f)
        assertEquals(0f, progressFraction(30_000L, -1L), 0.001f)
    }

    @Test
    fun `progress fraction clamps out of range positions`() {
        assertEquals(0f, progressFraction(-5_000L, 60_000L), 0.001f)
        assertEquals(1f, progressFraction(90_000L, 60_000L), 0.001f)
    }

    @Test
    fun `seek latch clears once the player catches up`() {
        assertTrue(shouldClearSeekTarget(elapsedMs = 100L, diffFraction = 0.01f))
        assertTrue(shouldClearSeekTarget(elapsedMs = 100L, diffFraction = 0.039f))
    }

    @Test
    fun `seek latch clears on timeout even when the player lags`() {
        assertTrue(shouldClearSeekTarget(elapsedMs = 5_001L, diffFraction = 0.5f))
    }

    @Test
    fun `seek latch holds while recent and far`() {
        assertFalse(shouldClearSeekTarget(elapsedMs = 100L, diffFraction = 0.5f))
        assertFalse(shouldClearSeekTarget(elapsedMs = 5_000L, diffFraction = 0.04f))
    }
}
