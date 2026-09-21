package org.evoionosp.noveliq.presentation.player

import org.evoionosp.noveliq.playback.SleepTimerState
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepSheetTest {
    @Test
    fun `sleep duration always renders minutes and seconds`() {
        assertEquals("1:00", formatSleepDuration(1))
        assertEquals("15:00", formatSleepDuration(15))
        assertEquals("30:00", formatSleepDuration(30))
        assertEquals("100:00", formatSleepDuration(100))
    }

    @Test
    fun `sleep countdown rounds up to the live second`() {
        assertEquals("30:00", formatSleepRemaining(1_800_000))
        assertEquals("1:30", formatSleepRemaining(90_000))
        assertEquals("1:01", formatSleepRemaining(61_000))
        assertEquals("0:05", formatSleepRemaining(5_000))
        assertEquals("0:01", formatSleepRemaining(500))
        assertEquals("0:00", formatSleepRemaining(0))
        assertEquals("0:00", formatSleepRemaining(-1_000))
        assertEquals("100:00", formatSleepRemaining(6_000_000))
    }

    @Test
    fun `sleep presets use compact minute labels`() {
        assertEquals("15m", formatSleepPreset(15))
        assertEquals("30m", formatSleepPreset(30))
        assertEquals("45m", formatSleepPreset(45))
        assertEquals("60m", formatSleepPreset(60))
        assertEquals("90m", formatSleepPreset(90))
    }

    @Test
    fun `sleep slider snaps to nearby presets`() {
        assertEquals(15, snapSleepMinutes(15f))
        assertEquals(15, snapSleepMinutes(16.5f))
        assertEquals(15, snapSleepMinutes(17f))
        assertEquals(30, snapSleepMinutes(31.9f))
        assertEquals(45, snapSleepMinutes(44.2f))
        assertEquals(60, snapSleepMinutes(59.4f))
        assertEquals(90, snapSleepMinutes(89f))
    }

    @Test
    fun `sleep slider rounds and clamps away from presets`() {
        assertEquals(17, snapSleepMinutes(17.1f))
        assertEquals(34, snapSleepMinutes(33.7f))
        assertEquals(48, snapSleepMinutes(47.5f))
        assertEquals(1, snapSleepMinutes(0.2f))
        assertEquals(100, snapSleepMinutes(100f))
        assertEquals(100, snapSleepMinutes(200f))
    }

    @Test
    fun `footer label shows countdown while armed`() {
        assertEquals("Zz", sleepFooterLabel(SleepTimerState.Off, null))
        assertEquals("Ch", sleepFooterLabel(SleepTimerState.EndOfChapter(50.0), null))
        assertEquals(
            "30m",
            sleepFooterLabel(SleepTimerState.Timer(endsAtMs = 1_801_000L), 1_800_000L),
        )
        assertEquals(
            "2m",
            sleepFooterLabel(SleepTimerState.Timer(endsAtMs = 91_000L), 90_000L),
        )
        assertEquals(
            "1m",
            sleepFooterLabel(SleepTimerState.Timer(endsAtMs = 500L), 0L),
        )
        assertEquals(
            "1m",
            sleepFooterLabel(SleepTimerState.Timer(endsAtMs = 500L), null),
        )
    }
}
