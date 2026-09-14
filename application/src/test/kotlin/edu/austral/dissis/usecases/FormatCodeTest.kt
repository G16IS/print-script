package edu.austral.dissis.usecases

import edu.austral.dissis.testing.FormatExample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.util.Result

class FormatCodeTest {
    @Test
    fun `formats an unformatted expression file`() {
        val formatted = FormatExample.format("unformatted_expression.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("1 + 2;\n", (formatted as Result.Ok).value)
    }

    @Test
    fun `formats an unformatted declaration file`() {
        val formatted = FormatExample.format("unformatted_declaration.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("let x : number = 1;\n", (formatted as Result.Ok).value)
    }

    @Test
    fun `formats an uninitialized declaration without assign`() {
        val formatted = FormatExample.format("unformatted_uninitialized.ps")

        assertTrue(formatted is Result.Ok)
        assertEquals("let x : string;\n", (formatted as Result.Ok).value)
    }
}
