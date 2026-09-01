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
}
