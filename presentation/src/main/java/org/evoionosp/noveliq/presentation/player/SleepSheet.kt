package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.evoionosp.noveliq.domain.settings.AppSettings
import org.evoionosp.noveliq.playback.SleepTimerState
import org.evoionosp.noveliq.presentation.R

private val SLEEP_PRESETS = listOf(15, 30, 45, 60, 90)

// Magnet window around a preset. Stays well below the preset gaps so every
// minute between presets remains selectable.
private const val PRESET_SNAP_THRESHOLD_MINUTES = 2f

/** Staged duration as "M:SS" — "30:00", "100:00". Never hours: the 100-minute cap keeps it short. */
internal fun formatSleepDuration(minutes: Int): String = "$minutes:00"

/**
 * Running countdown, always "M:SS" — ceiling seconds so a live timer never
 * reads "0:00" before it fires.
 */
internal fun formatSleepRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs.coerceAtLeast(0) + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Preset labels: "15m", "30m", "45m", "60m", "90m". */
internal fun formatSleepPreset(minutes: Int): String = "${minutes}m"

/**
 * Snaps a raw slider value: magnet to a preset when close, otherwise the
 * nearest whole minute inside the 1–100 range.
 */
internal fun snapSleepMinutes(raw: Float): Int {
    val nearestPreset = SLEEP_PRESETS.minBy { abs(it - raw) }
    return if (abs(nearestPreset - raw) <= PRESET_SNAP_THRESHOLD_MINUTES) {
        nearestPreset
    } else {
        raw.roundToInt().coerceIn(
            AppSettings.MIN_SLEEP_TIMER_MINUTES,
            AppSettings.MAX_SLEEP_TIMER_MINUTES,
        )
    }
}

/** Footer label: "Zz" idle, remaining "42m" on a timer, "Ch" at chapter end. */
internal fun sleepFooterLabel(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
): String =
    when (sleepTimer) {
        SleepTimerState.Off -> {
            "Zz"
        }

        is SleepTimerState.Timer -> {
            val minutes = (((remainingMs ?: 0L) + 59_999) / 60_000).coerceAtLeast(1)
            "${minutes}m"
        }

        is SleepTimerState.EndOfChapter -> {
            "Ch"
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(
    sleepTimer: SleepTimerState,
    sleepRemainingMs: Long?,
    currentChapterTitle: String?,
    endOfChapterAvailable: Boolean,
    persistedMinutes: Int,
    onArmTimer: (Int) -> Unit,
    onArmEndOfChapter: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pc = LocalPlayerColors.current
    val timerArmed = sleepTimer is SleepTimerState.Timer
    val chapterArmed = sleepTimer is SleepTimerState.EndOfChapter

    // Staged selection, synced to the armed state whenever it changes (and on
    // open): reopening the sheet while a 15-minute timer runs stages 15, while
    // a fresh sheet stages the last persisted pick. Saveable like the sheet
    // visibility: rotation must not lose it.
    var minutes by rememberSaveable(sleepTimer, persistedMinutes) {
        mutableStateOf(stagedSleepMinutes(sleepTimer, sleepRemainingMs, persistedMinutes))
    }

    // The manual timer and the chapter switch are independent options that
    // merely exclude each other: timer controls live only when idle, and the
    // chapter switch stays interactive while on so it can be turned back off.
    val timerControlsEnabled = sleepTimer == SleepTimerState.Off

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Tall content (chapter row + slider + presets + start button): open
        // fully instead of stopping at the half-height partial state.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalPlayerCardColor.current,
        contentColor = pc.textPrimary,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = pc.textSecondary.copy(alpha = 0.4f))
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.now_playing_sleep_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = pc.textPrimary,
            )

            SleepChapterRow(
                chapterTitle = currentChapterTitle,
                checked = chapterArmed,
                enabled = endOfChapterAvailable && !timerArmed,
                onCheckedChange = { checked ->
                    if (checked) {
                        onArmEndOfChapter()
                    } else {
                        onCancel()
                    }
                },
            )

            Text(
                text = sleepHeadline(sleepTimer, sleepRemainingMs, minutes),
                style = MaterialTheme.typography.headlineMedium,
                color = pc.accent,
            )

            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = snapSleepMinutes(it) },
                onValueChangeFinished = {
                    // The slider only stages: a tap on Start arms it. Persist
                    // on release rather than every frame of the drag.
                    onDurationChange(minutes)
                },
                valueRange =
                    AppSettings.MIN_SLEEP_TIMER_MINUTES.toFloat()..AppSettings.MAX_SLEEP_TIMER_MINUTES.toFloat(),
                enabled = timerControlsEnabled,
                colors =
                    SliderDefaults.colors(
                        thumbColor = pc.accent,
                        activeTrackColor = pc.accent,
                        inactiveTrackColor = pc.trackSubtle,
                    ),
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val presetColors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = pc.accent,
                        activeContentColor = pc.onAccent,
                        activeBorderColor = pc.accent,
                        inactiveContentColor = pc.textSecondary,
                        inactiveBorderColor = pc.trackSubtle,
                    )
                SLEEP_PRESETS.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, SLEEP_PRESETS.size),
                        onClick = {
                            minutes = preset
                            onDurationChange(preset)
                        },
                        selected = timerControlsEnabled && minutes == preset,
                        enabled = timerControlsEnabled,
                        label = { Text(text = formatSleepPreset(preset)) },
                        colors = presetColors,
                        // No check icon: the accent fill already marks selection.
                        icon = {},
                    )
                }
            }

            Button(
                onClick = {
                    if (timerArmed) onCancel() else onArmTimer(minutes)
                },
                // Chapter mode owns stopping itself via its switch; the button
                // only ever starts or stops the manual timer.
                enabled = !chapterArmed,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    if (timerArmed) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = pc.accent,
                            contentColor = pc.onAccent,
                        )
                    },
            ) {
                Text(
                    text =
                        stringResource(
                            if (timerArmed) {
                                R.string.now_playing_sleep_stop_timer
                            } else {
                                R.string.now_playing_sleep_start_timer
                            },
                        ),
                )
            }
        }
    }
}

/**
 * Staged minutes for a fresh sheet: remaining on a running timer, else the
 * last persisted pick. Always inside the slider range.
 */
private fun stagedSleepMinutes(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
    persistedMinutes: Int,
): Int {
    val remaining =
        if (sleepTimer is SleepTimerState.Timer) {
            remainingMs
        } else {
            null
        }
    return if (remaining != null) {
        ((remaining + 59_999) / 60_000).toInt()
    } else {
        persistedMinutes
    }.coerceIn(
        AppSettings.MIN_SLEEP_TIMER_MINUTES,
        AppSettings.MAX_SLEEP_TIMER_MINUTES,
    )
}

@Composable
private fun sleepHeadline(
    sleepTimer: SleepTimerState,
    remainingMs: Long?,
    minutes: Int,
): String {
    val chapterLabel = stringResource(R.string.now_playing_sleep_end_of_chapter)
    return when (sleepTimer) {
        is SleepTimerState.Timer -> {
            formatSleepRemaining(remainingMs ?: 0L)
        }

        is SleepTimerState.EndOfChapter -> {
            chapterLabel
        }

        SleepTimerState.Off -> {
            formatSleepDuration(minutes)
        }
    }
}

@Composable
private fun SleepChapterRow(
    chapterTitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val pc = LocalPlayerColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.now_playing_sleep_stop_at_end_of_chapter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = pc.textPrimary,
            )
            if (chapterTitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = pc.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = pc.onAccent,
                    checkedTrackColor = pc.accent,
                    uncheckedThumbColor = pc.textSecondary,
                ),
        )
    }
}
