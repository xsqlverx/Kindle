package com.example

import com.example.data.model.AssistantOrbBounds
import com.example.data.model.AssistantOrbPlacement
import org.junit.Assert.*
import org.junit.Test

class AssistantOrbPositionTest {
    @Test fun `drag snaps to closest edge and keeps the clamped height`() {
        val bounds = AssistantOrbBounds(400f, 800f, 60f, 100f, 140f, 8f)
        val left = bounds.snap(-30f, 250f)
        val right = bounds.snap(999f, 900f)
        assertTrue(left.left)
        assertFalse(right.left)
        assertEquals(bounds.leftX, bounds.x(left), 0.001f)
        assertEquals(bounds.rightX, bounds.x(right), 0.001f)
        assertEquals(250f, bounds.y(left), 0.001f)
        assertEquals(bounds.bottomY, bounds.y(right), 0.001f)
    }

    @Test fun `relative height survives a resize and tiny screens stay bounded`() {
        val tall = AssistantOrbBounds(400f, 800f, 60f, 100f, 100f, 8f)
        val small = AssistantOrbBounds(250f, 440f, 60f, 80f, 120f, 8f)
        val position = tall.snap(320f, 340f)
        val resized = small.snap(small.x(position), small.y(position))
        assertEquals(position.verticalFraction, resized.verticalFraction, 0.001f)
        val tiny = AssistantOrbBounds(30f, 35f, 60f, 200f, 300f, 8f)
        val result = tiny.snap(Float.NaN, Float.NaN)
        assertTrue(tiny.x(result).isFinite())
        assertTrue(tiny.y(result).isFinite())
    }

    @Test fun `invalid saved fraction falls back to the middle`() {
        val bounds = AssistantOrbBounds(400f, 800f, 60f, 100f, 100f, 8f)
        assertEquals(bounds.y(AssistantOrbPlacement(true, 0.5f)), bounds.y(AssistantOrbPlacement(true, Float.NaN)), 0.001f)
    }
}
