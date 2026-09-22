package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.support.loc

class ErrorFormattingTest {
    private val location = loc(startLine = 2, startCol = 3, endLine = 2, endCol = 9)

    @Test
    fun `formatLocated includes the message and start-end span`() {
        assertEquals(
            "boom (2:3-2:9)",
            formatLocated("boom", location),
        )
    }

    @Test
    fun `formatError formats FormatError LexerError ParserError LintError RuntimeError and TypeError`() {
        assertEquals(
            "El token 'SEMICOLON' no tiene lexema para emitir (2:3-2:9)",
            formatError(MissingLexeme("SEMICOLON", location)),
        )
        assertEquals(
            "Unexpected token at line 2 col 3 (2:3-2:9)",
            formatError(UnexpectedToken(location)),
        )
        assertEquals(
            "Unexpected token at line 2 col 3 (2:3-2:9)",
            formatError(MissingToken(location)),
        )
        assertEquals(
            "La llamada a println solo acepta un identificador o un literal (2:3-2:9)",
            formatError(InvalidPrintlnArgument(location)),
        )
        assertEquals(
            "División por cero (2:3-2:9)",
            formatError(DivisionByZero(location)),
        )
        assertEquals(
            "Tipo desconocido 'boolean' (2:3-2:9)",
            formatError(UnknownType("boolean", location)),
        )
    }
}
