package printscript

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.syntax.Location

class TokenRegistryTest {
    private val start = CharPosition(1, 1)
    private val end = CharPosition(1, 3)

    @Test
    fun `hasToken is true only for registered lexemes`() {
        val registry = TokenRegistry(mapOf("let" to { location -> Token("LET", Optional.empty(), location) }))

        assertTrue(registry.hasToken("let"))
        assertFalse(registry.hasToken("var"))
    }

    @Test
    fun `getToken builds a token with the given span`() {
        val registry =
            TokenRegistry(
                mapOf(
                    "let" to { location -> Token("LET", Optional.empty(), location) },
                ),
            )
        val token = registry.getToken("let", start, end)

        assertEquals("LET", token.type)
        assertEquals(Location(start, end), token.location)
    }

    @Test
    fun `getToken rejects an unknown lexeme`() {
        val registry = TokenRegistry(emptyMap())

        val error =
            assertThrows<IllegalArgumentException> {
                registry.getToken("nope", start, end)
            }
        assertTrue(error.message!!.contains("Unexpected token"))
    }
}
