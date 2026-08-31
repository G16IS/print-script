package edu.austral.dissis.usecases

import edu.austral.dissis.testing.FormatExample
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CheckFormatTest {
    @Test
    fun `unformatted file fails the format check`() {
        val error =
            assertThrows<IllegalStateException> {
                FormatExample.check("unformatted_expression.ps")
            }

        assertTrue(error.message!!.contains("El chequeo de formato falló"))
    }

    @Test
    fun `formatted file passes the format check`() {
        assertDoesNotThrow {
            FormatExample.check("formatted_expression.ps")
        }
    }

    @Test
    fun `unformatted file lists each whitespace mismatch`() {
        val error =
            assertThrows<IllegalStateException> {
                FormatExample.check("unformatted_declaration.ps")
            }

        assertTrue(error.message!!.contains("El chequeo de formato falló"))
        assertTrue(error.message!!.contains("Se esperaba whitespace"))
        assertTrue(error.message!!.count { it == '\n' } >= 4)
    }

    @Test
    fun `formatted declaration passes the format check`() {
        assertDoesNotThrow {
            FormatExample.check("formatted_declaration.ps")
        }
    }
}
