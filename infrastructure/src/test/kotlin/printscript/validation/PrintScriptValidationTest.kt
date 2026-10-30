package printscript.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.error.NoRulesProvided
import printscript.error.TypeMismatch
import printscript.error.UndeclaredIdentifier
import printscript.reader.CharPosition
import printscript.syntax.Location

class PrintScriptValidationTest {
    private fun checked(
        version: String,
        source: String,
    ): List<ValidationIssue> {
        val outcome = PrintScriptValidation.validate(version, source)
        return (outcome as ValidationOutcome.Checked).issues
    }

    @Test
    fun `a valid program has no issues`() {
        val outcome = PrintScriptValidation.validate("1.0", "let x: number = 1;\nprintln(x);")

        assertTrue(outcome is ValidationOutcome.Checked)
        assertTrue((outcome as ValidationOutcome.Checked).isValid)
    }

    @Test
    fun `a program that prints is only analyzed, never run`() {
        // Si se ejecutara, el manager sin handlers lanzaría "No handler found".
        assertEquals(emptyList<ValidationIssue>(), checked("1.0", "println(\"hello\");"))
    }

    @Test
    fun `an empty source is valid`() {
        assertEquals(emptyList<ValidationIssue>(), checked("1.0", ""))
    }

    @Test
    fun `a type mismatch says what it is and where it starts`() {
        val issues = checked("1.0", "let x: number = \"hola\";")

        assertEquals(1, issues.size)
        assertEquals("type-mismatch", issues[0].code)
        assertEquals(1, issues[0].line)
        assertTrue(issues[0].message.isNotBlank())
    }

    @Test
    fun `an undeclared variable is reported on its own line`() {
        val issues = checked("1.0", "let x: number = 1;\nprintln(y);")

        assertEquals(1, issues.size)
        assertEquals("undeclared-identifier", issues[0].code)
        assertEquals(2, issues[0].line)
    }

    @Test
    fun `a redeclaration is reported`() {
        val issues = checked("1.0", "let x: number = 1;\nlet x: string = \"a\";")

        assertEquals(listOf("redeclaration"), issues.map { it.code })
        assertEquals(2, issues[0].line)
    }

    @Test
    fun `type errors accumulate and keep their order`() {
        val source = "let x: number = \"hola\";\nprintln(x);\nlet z: number = \"no\";"

        val issues = checked("1.0", source)

        assertEquals(listOf("type-mismatch", "type-mismatch"), issues.map { it.code })
        assertEquals(listOf(1, 3), issues.map { it.line })
    }

    @Test
    fun `a syntax error is reported with a position`() {
        val issues = checked("1.0", "let x number = 1;")

        assertEquals(1, issues.size)
        assertTrue(issues[0].line >= 1)
        assertTrue(issues[0].column >= 1)
    }

    @Test
    fun `positions count from one and follow the lines`() {
        val issues = checked("1.0", "let a: number = 1;\n\n\nprintln(nope);")

        assertEquals(4, issues[0].line)
    }

    @Test
    fun `a version that does not exist is unsupported`() {
        listOf("9.9", "1", "abc", "", "01.1", "1.1.0").forEach { version ->
            assertEquals(
                ValidationOutcome.UnsupportedVersion(version),
                PrintScriptValidation.validate(version, "let x: number = 1;"),
                "version '$version'",
            )
        }
    }

    @Test
    fun `both published versions are supported`() {
        assertEquals(emptyList<ValidationIssue>(), checked("1.0", "let x: number = 1;"))
        assertEquals(emptyList<ValidationIssue>(), checked("1.1", "let x: number = 1;"))
    }

    @Test
    fun `a feature of 1_1 is accepted by 1_1 and rejected by 1_0`() {
        val source = "const x: number = 1;"

        assertEquals(emptyList<ValidationIssue>(), checked("1.1", source))
        assertTrue(checked("1.0", source).isNotEmpty())
    }

    @Test
    fun `errors of the language configuration are not reported as issues of the code`() {
        assertThrows<IllegalStateException> { NoRulesProvided().toIssue() }
    }

    @Test
    fun `an error without a position cannot become an issue`() {
        val noPosition = Location(CharPosition(0, 0), CharPosition(0, 0))

        assertThrows<IllegalStateException> { TypeMismatch("number", "string", noPosition).toIssue() }
    }

    @Test
    fun `an error that cannot come out of a check is a bug`() {
        val error = printscript.error.DivisionByZero(Location(CharPosition(1, 1), CharPosition(1, 2)))

        assertThrows<IllegalStateException> { error.toIssue() }
    }

    @Test
    fun `an error with a position becomes an issue with its code`() {
        val at = Location(CharPosition(3, 7), CharPosition(3, 8))

        assertEquals(
            ValidationIssue("undeclared-identifier", "Variable 'y' no declarada", 3, 7),
            UndeclaredIdentifier("y", at).toIssue(),
        )
    }
}
