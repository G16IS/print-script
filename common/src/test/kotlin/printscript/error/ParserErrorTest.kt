package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.support.loc

class ParserErrorTest {
    private val location = loc(2, 3, 2, 4)

    @Test
    fun `MissingToken default message includes line and column`() {
        val error = MissingToken(location)

        assertEquals("Unexpected token at line 2 col 3", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `MissingToken can override the message`() {
        val error = MissingToken(location, message = "expected SEMICOLON")

        assertEquals("expected SEMICOLON", error.message)
    }

    @Test
    fun `UnexpectedStart default message includes line and column`() {
        val error = UnexpectedStart(location)

        assertEquals("Unexpected token at line 2 col 3", error.message)
    }

    @Test
    fun `UnexpectedStart can override the message`() {
        val error = UnexpectedStart(location, message = "expected statement")

        assertEquals("expected statement", error.message)
    }
}
