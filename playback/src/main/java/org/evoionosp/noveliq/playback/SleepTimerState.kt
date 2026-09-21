package org.evoionosp.noveliq.playback

/**
 * Sleep timer arming. Timer mode counts wall-clock time down from [endsAtMs];
 * EndOfChapter pauses when the book position reaches [targetBookSeconds].
 */
sealed interface SleepTimerState {
    data object Off : SleepTimerState

    data class Timer(
        val endsAtMs: Long,
    ) : SleepTimerState

    data class EndOfChapter(
        val targetBookSeconds: Double,
    ) : SleepTimerState
}

/**
 * Milliseconds left on a wall-clock timer, or null when no timer countdown
 * is running. Pure so the UI countdown and tests share one rule.
 */
fun SleepTimerState.remainingMs(nowMs: Long): Long? =
    when (this) {
        SleepTimerState.Off -> null
        is SleepTimerState.Timer -> (endsAtMs - nowMs).coerceAtLeast(0)
        is SleepTimerState.EndOfChapter -> null
    }
