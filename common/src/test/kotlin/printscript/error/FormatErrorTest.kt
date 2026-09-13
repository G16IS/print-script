package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.ast.Location
import printscript.support.loc

class FormatErrorTest {
    private val location = loc(1, 1, 1, 2)

    @Test
    fun `MissingLexeme names the token type`() {
        val error = MissingLexeme("SEMICOLON", location)

        assertEquals("El token 'SEMICOLON' no tiene lexema para emitir", error.message)
    }

    @Test
    fun `UnrecognizedFormatNode names the node`() {
        val error = UnrecognizedFormatNode("if", location)

        assertEquals("Nodo no reconocido 'if'", error.message)
    }

    @Test
    fun `WhitespaceMismatch shows escaped newlines and tabs`() {
        val error = WhitespaceMismatch(expected = "\n\t", actual = "  ", location = location)

        assertEquals(
            "Se esperaba whitespace \"\\n\\t\" pero se encontró \"  \"",
            error.message,
        )
    }

    @Test
    fun `WhitespaceMismatch without special chars still quotes the text`() {
        val error = WhitespaceMismatch(expected = " ", actual = "", location = location)

        assertEquals(
            "Se esperaba whitespace \" \" pero se encontró \"\"",
            error.message,
        )
    }

    @Test
    fun `UnknownRuleType defaults location to empty`() {
        val error = UnknownRuleType(type = "indent")

        assertEquals(Location.empty(), error.location)
        assertEquals("Tipo de regla de formato desconocido: 'indent'", error.message)
    }

    @Test
    fun `InvalidRuleParams includes the reason and defaults location to empty`() {
        val error = InvalidRuleParams(type = "space-before", reason = "count must be >= 0")

        assertEquals(Location.empty(), error.location)
        assertEquals(
            "Parámetros inválidos para 'space-before': count must be >= 0",
            error.message,
        )
    }
}
