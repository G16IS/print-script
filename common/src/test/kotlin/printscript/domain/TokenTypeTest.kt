package printscript.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class TokenTypeTest {
    @Test
    fun `enum still lists the leftover lexical categories`() {
        val names = TokenType.entries.map { it.name }

        assertTrue(names.containsAll(listOf("LET", "IDENTIFIER", "NUMBER_LITERAL", "EOF")))
    }
}
