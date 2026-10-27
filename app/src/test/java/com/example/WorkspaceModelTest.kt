package com.example

import com.example.data.model.DayPlanner
import com.example.data.model.Destination
import org.junit.Assert.*
import org.junit.Test

class WorkspaceModelTest {
    @Test fun `every destination can occupy every outer slot`() {
        Destination.entries.forEach { destination -> (0..3).forEach { index ->
            val slots = Destination.assign(Destination.defaults, index, destination)
            assertEquals(destination, slots[index])
            assertEquals(4, slots.distinct().size)
        } }
    }
    @Test fun `choosing an existing pin swaps instead of dropping another slot`() {
        val slots = Destination.assign(Destination.defaults, 0, Destination.FITNESS)
        assertEquals(Destination.FITNESS, slots[0])
        assertEquals(Destination.TODAY, slots[3])
    }
    @Test fun `invalid duplicated and future IDs repair to four unique known destinations`() {
        val slots = Destination.validateSlots(listOf("settings", "settings", "unknown", "college"))
        assertEquals(Destination.SETTINGS, slots[0])
        assertEquals(Destination.COLLEGE, slots[1])
        assertEquals(4, slots.distinct().size)
        assertEquals(slots, Destination.validateSlots(slots.map { it.id }))
    }
    @Test fun `a free window may end exactly at a commitment`() {
        assertEquals(0L, DayPlanner.nextWindow(0, 3_600_000, 25, listOf(1_500_000L to 3_000_000L)))
    }
    @Test fun `an ongoing fixed commitment pushes work past its end`() {
        assertEquals(3_000_000L, DayPlanner.nextWindow(2_000_000, 6_000_000, 25, listOf(1_500_000L to 3_000_000L)))
    }
    @Test fun `overlapping intervals and unsorted blocks cannot create a false gap`() {
        assertEquals(4_000_000L, DayPlanner.nextWindow(0, 6_000_000, 25,
            listOf(1_000_000L to 4_000_000L, 0L to 2_000_000L)))
    }
    @Test fun `no room returns null rather than crossing the protected horizon`() {
        assertNull(DayPlanner.nextWindow(0, 1_000_000, 25, emptyList()))
        assertNull(DayPlanner.nextWindow(0, 3_000_000, 25, listOf(0L to 3_000_000L)))
    }
    @Test fun `invalid duration or reversed window has no feasible result`() {
        assertNull(DayPlanner.nextWindow(0, 10_000_000, 0, emptyList()))
        assertNull(DayPlanner.nextWindow(10_000_000, 0, 25, emptyList()))
    }
}
