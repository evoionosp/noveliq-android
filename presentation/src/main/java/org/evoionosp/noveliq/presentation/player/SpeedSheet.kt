package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.evoionosp.noveliq.presentation.R

private const val MIN_SPEED = 0.5f
private const val MAX_SPEED = 3.0f
private const val SPEED_STEP = 0.05f

// Magnet window around a major stop. Must stay below SPEED_STEP so the
// neighboring 0.05 values (e.g. 1.05 next to the 1.0 stop) stay reachable.
private const val STOP_SNAP_THRESHOLD = 0.04f

private val SPEED_STOPS = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

// One-tap presets. These set exact values and are independent of the magnet
// stops (0.9 is on the 0.05 grid, so it stays draggable too).
private val SPEED_PRESETS = listOf(0.75f, 0.9f, 1.0f, 1.25f, 1.5f)

/** 0.5x, 1x, 1.25x, 1.5x, 2x … — integers render clean, fractions keep decimals. */
private fun Float.formatSpeedLabel(): String = "%.2f".format(this).trimEnd('0').trimEnd('.') + "x"

/**
 * Snaps a raw drag value: magnet to a major stop when close, otherwise
 * quantize to the 0.05 grid so every micro-step stays selectable.
 */
private fun Float.snapPlaybackSpeed(): Float {
    val nearestStop = SPEED_STOPS.minBy { abs(it - this) }
    return if (abs(nearestStop - this) <= STOP_SNAP_THRESHOLD) {
        nearestStop
    } else {
        (this / SPEED_STEP).roundToInt() * SPEED_STEP
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
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
                text = stringResource(R.string.now_playing_speed_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = "${"%.2f".format(speed)}x",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Slider(
                value = speed.coerceIn(MIN_SPEED, MAX_SPEED),
                // steps = 0 keeps the drag smooth; snapPlaybackSpeed() provides
                // the only detents, at the magnet stops.
                onValueChange = { onSpeedChange(it.snapPlaybackSpeed()) },
                valueRange = MIN_SPEED..MAX_SPEED,
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SPEED_PRESETS.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, SPEED_PRESETS.size),
                        onClick = { onSpeedChange(preset) },
                        // Epsilon, not ==: a drag-quantized value can carry float dust.
                        selected = abs(speed - preset) < 0.001f,
                        label = { Text(text = preset.formatSpeedLabel()) },
                    )
                }
            }
        }
    }
}
