package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.util.Report

class LintErrorTest {
    private val location =
        Location(
            start = CharPosition(1, 2),
            end = CharPosition(1, 8),
        )

    @Test
    fun `InvalidIdentifierFormat carries identifier, format, and Spanish message`() {
        val error =
            InvalidIdentifierFormat(
                identifier = "snake_var",
                expectedFormat = "camelCase",
                location = location,
            )

        assertEquals(location, error.location)
        assertEquals("El identificador 'snake_var' no respeta el formato camelCase", error.message)
    }

    @Test
    fun `InvalidPrintlnArgument carries location and message`() {
        val error = InvalidPrintlnArgument(location = location)

        assertEquals(location, error.location)
        assertEquals("La llamada a println solo acepta un identificador o un literal", error.message)
    }

    @Test
    fun `Report is not ok when it contains LintErrors`() {
        val error = InvalidPrintlnArgument(location = location)
        val report = Report(value = "program", errors = listOf(error))

        assertFalse(report.isOk)
        assertEquals(error, report.errors.single())
    }
}
