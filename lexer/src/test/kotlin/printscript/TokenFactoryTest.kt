package printscript

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import printscript.ast.Location
import printscript.domain.ExactRule
import printscript.domain.RegexRule
import printscript.reader.CharPosition

class TokenFactoryTest {
    private val location = Location(CharPosition(0, 1), CharPosition(0, 3))

    @Test
    fun `type comes from the rule`() {
        val token = TokenFactory.create(ExactRule(listOf("let"), "LET", false), location, "let")
        assertEquals("LET", token.type)
    }

    @Test
    fun `capture true stores the lexeme`() {
        val rule =
            RegexRule(
                matcher = listOf("^[a-zA-Z_][a-zA-Z0-9_]*"),
                token = "ID",
                capture = true,
                partial = "^[a-zA-Z_]",
            )
        val token = TokenFactory.create(rule, location, "pepe")
        assertEquals(Optional.of("pepe"), token.value)
    }

    @Test
    fun `capture false leaves the value empty`() {
        val token = TokenFactory.create(ExactRule(listOf("let"), "LET", false), location, "let")
        assertEquals(Optional.empty<String>(), token.value)
    }

    @Test
    fun `location is forwarded`() {
        val token = TokenFactory.create(ExactRule(listOf(";"), "SEMICOLON", false), location, ";")
        assertEquals(location, token.location)
    }
}
