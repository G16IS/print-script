package printscript.domain

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import printscript.support.loc

class TokenTest {
    @Test
    fun `stores type, captured value, and location`() {
        val location = loc(1, 5, 1, 8)
        val token = Token("ID", Optional.of("pepe"), location)

        assertEquals("ID", token.type)
        assertEquals("pepe", token.value.get())
        assertEquals(location, token.location)
    }

    @Test
    fun `keywords keep an empty value`() {
        val token = Token("LET", Optional.empty(), loc())

        assertFalse(token.value.isPresent)
    }

    @Test
    fun `ExactRule exposes matcher token and capture`() {
        val rule = ExactRule(listOf("let"), "LET", capture = false)

        assertEquals(listOf("let"), rule.matcher)
        assertEquals("LET", rule.token)
        assertFalse(rule.capture)
    }

    @Test
    fun `RegexRule keeps the partial pattern`() {
        val rule =
            RegexRule(
                matcher = listOf("^[0-9]+$"),
                token = "NUMBER_LITERAL",
                capture = true,
                partial = "^[0-9]",
            )

        assertEquals("NUMBER_LITERAL", rule.token)
        assertTrue(rule.capture)
        assertEquals("^[0-9]", rule.partial)
    }
}
