package edu.austral.dissis.usecases

import edu.austral.dissis.testing.FormatExample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatCodeTest {
    @Test
    fun `formats an unformatted expression file`() {
        val formatted = FormatExample.format("unformatted_expression.ps")

        assertEquals("1 + 2;\n", formatted)
    }
}
