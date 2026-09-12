package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold
import printscript.util.isOk

class TypeErrorTest {
    private val location =
        Location(
            start = CharPosition(1, 2),
            end = CharPosition(1, 8),
        )

    @Test
    fun `UnknownType carries location and a Spanish message`() {
        val error = UnknownType(typeName = "boolean", location = location)

        assertEquals(location, error.location)
        assertEquals("Tipo desconocido 'boolean'", error.message)
    }

    @Test
    fun `TypeMismatch carries expected and actual types`() {
        val error =
            TypeMismatch(
                expected = "number",
                actual = "string",
                location = location,
            )

        assertEquals(location, error.location)
        assertEquals("Se esperaba number pero se encontró string", error.message)
    }

    @Test
    fun `Redeclaration names the variable`() {
        val error = Redeclaration(name = "age", location = location)

        assertEquals("La variable 'age' ya fue declarada", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `UndeclaredIdentifier names the variable`() {
        val error = UndeclaredIdentifier(name = "missing", location = location)

        assertEquals("Variable 'missing' no declarada", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `InvalidOperands describes operator and operand types`() {
        val error =
            InvalidOperands(
                operator = "+",
                left = "number",
                right = "string",
                location = location,
            )

        assertEquals("El operador '+' no acepta number y string", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `UnrecognizedNode names the syntax node`() {
        val error = UnrecognizedNode(nodeName = "if", location = location)

        assertEquals("Nodo no reconocido 'if'", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `TypeError can be the error of a Result`() {
        val error = UndeclaredIdentifier(name = "x", location = location)
        val result: Result<String, TypeError> = Result.Err(error)

        assertFalse(result.isOk)
        assertIs<UndeclaredIdentifier>(
            result.fold(
                onOk = { error("expected Err") },
                onErr = { it },
            ),
        )
    }

    @Test
    fun `Report is not ok when it contains TypeErrors`() {
        val error = TypeMismatch(expected = "string", actual = "number", location = location)
        val report = Report(value = "program", errors = listOf(error))

        assertFalse(report.isOk)
        assertEquals(error, report.errors.single())
    }
}
