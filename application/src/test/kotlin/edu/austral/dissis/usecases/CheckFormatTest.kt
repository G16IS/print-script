package edu.austral.dissis.usecases

import edu.austral.dissis.testing.FormatExample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.WhitespaceMismatch

class CheckFormatTest {
    @Test
    fun `unformatted file fails the format check`() {
        val report = FormatExample.check("unformatted_expression.ps")

        assertFalse(report.isOk)
        assertTrue(report.errors.any { it is WhitespaceMismatch })
    }

    @Test
    fun `formatted file passes the format check`() {
        val report = FormatExample.check("formatted_expression.ps")

        assertTrue(report.isOk)
        assertEquals(0, report.errors.size)
    }

    @Test
    fun `unformatted file lists each whitespace mismatch`() {
        val report = FormatExample.check("unformatted_declaration.ps")

        assertFalse(report.isOk)
        assertTrue(report.errors.all { it is WhitespaceMismatch })
        assertTrue(report.errors.size >= 4)
    }

    @Test
    fun `formatted declaration passes the format check`() {
        val report = FormatExample.check("formatted_declaration.ps")

        assertTrue(report.isOk)
        assertEquals(0, report.errors.size)
    }
}
