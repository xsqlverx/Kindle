package com.example

import com.example.data.model.AttendanceProjection
import org.junit.Assert.*
import org.junit.Test

class AttendanceProjectionTest {
    @Test fun `default target needs ten consecutive classes from six out of ten`() {
        assertEquals(10, AttendanceProjection(6, 10, 80).classesNeeded)
    }

    @Test fun `a configured 75 percent target does not use an 80 percent formula`() {
        val counts = AttendanceProjection(6, 10, 75)
        assertEquals(6, counts.classesNeeded)
    }

    @Test fun `buffer includes only whole absences that preserve the target`() {
        val counts = AttendanceProjection(9, 10, 80)
        assertEquals(1, counts.absenceBuffer)
        assertTrue(AttendanceProjection(9, 11, 80).isAtTarget)
        assertFalse(AttendanceProjection(9, 12, 80).isAtTarget)
    }

    @Test fun `perfect target with past absence cannot claim a finite recovery`() {
        assertNull(AttendanceProjection(9, 10, 100).classesNeeded)
    }

    @Test fun `empty baseline has no measured percentage`() {
        val counts = AttendanceProjection(0, 0, 80)
        assertNull(counts.percentage)
        assertFalse(counts.isAtTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `attended cannot exceed total`() { AttendanceProjection(11, 10, 80) }
}
