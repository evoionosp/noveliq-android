package org.evoionosp.noveliq.presentation.player

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControlsTest {
    @Test
    fun `paused corner is half the button size at every size`() {
        listOf(44.dp, 72.dp, 96.dp).forEach { size ->
            assertEquals(
                size.value / 2f,
                playPauseCorner(isPlaying = false, buttonSize = size).value,
                0.001f,
            )
        }
    }

    @Test
    fun `playing corner is a squircle smaller than the paused circle`() {
        listOf(44.dp, 72.dp, 96.dp).forEach { size ->
            val playing = playPauseCorner(isPlaying = true, buttonSize = size).value
            val paused = playPauseCorner(isPlaying = false, buttonSize = size).value
            assertTrue(playing > 0f)
            assertTrue(playing < paused)
        }
    }
}
