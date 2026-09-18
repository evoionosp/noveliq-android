package org.evoionosp.noveliq.presentation.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SheetDragStateTest {
    @Test
    fun `decisive drag up expands even from a low fraction`() {
        assertEquals(
            SheetTarget.EXPANDED,
            resolveSheetTarget(
                startFraction = 0.1f,
                endFraction = 0.3f,
                velocityYPxPerS = 0f,
            ),
        )
    }

    @Test
    fun `decisive drag down collapses even from a high fraction`() {
        assertEquals(
            SheetTarget.COLLAPSED,
            resolveSheetTarget(
                startFraction = 0.9f,
                endFraction = 0.7f,
                velocityYPxPerS = 0f,
            ),
        )
    }

    @Test
    fun `fling up expands when the drag was indecisive`() {
        assertEquals(
            SheetTarget.EXPANDED,
            resolveSheetTarget(
                startFraction = 0.4f,
                endFraction = 0.45f,
                velocityYPxPerS = -1000f,
            ),
        )
    }

    @Test
    fun `fling down collapses when the drag was indecisive`() {
        assertEquals(
            SheetTarget.COLLAPSED,
            resolveSheetTarget(
                startFraction = 0.6f,
                endFraction = 0.55f,
                velocityYPxPerS = 1000f,
            ),
        )
    }

    @Test
    fun `slow release falls back to the nearest anchor`() {
        assertEquals(
            SheetTarget.EXPANDED,
            resolveSheetTarget(
                startFraction = 0.6f,
                endFraction = 0.65f,
                velocityYPxPerS = 0f,
            ),
        )
        assertEquals(
            SheetTarget.COLLAPSED,
            resolveSheetTarget(
                startFraction = 0.4f,
                endFraction = 0.35f,
                velocityYPxPerS = 0f,
            ),
        )
    }

    @Test
    fun `exactly halfway collapses`() {
        assertEquals(
            SheetTarget.COLLAPSED,
            resolveSheetTarget(
                startFraction = 0.5f,
                endFraction = 0.5f,
                velocityYPxPerS = 0f,
            ),
        )
    }

    @Test
    fun `mini bar cannot be dragged below the collapsed anchor`() {
        val state = SheetDragState(CoroutineScope(Dispatchers.Unconfined), {}, {})
        state.onTravelKnown(1000f)
        assertEquals(1000f, state.offsetPx.floatValue, 0f)
        state.dragBy(500f)
        assertEquals(1000f, state.offsetPx.floatValue, 0f)
    }

    @Test
    fun `mid-sheet drags move freely between anchors`() {
        val state = SheetDragState(CoroutineScope(Dispatchers.Unconfined), {}, {})
        state.onTravelKnown(1000f)
        state.dragBy(-400f)
        assertEquals(600f, state.offsetPx.floatValue, 0f)
        state.dragBy(200f)
        assertEquals(800f, state.offsetPx.floatValue, 0f)
    }

    @Test
    fun `expanded sheet keeps top rubber-band overshoot`() {
        val state = SheetDragState(CoroutineScope(Dispatchers.Unconfined), {}, {})
        state.onTravelKnown(1000f)
        state.dragBy(-2000f)
        assertEquals(-80f, state.offsetPx.floatValue, 0f)
    }
}
