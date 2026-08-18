package printscript.evaluator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.grammar
import printscript.support.or
import printscript.support.parse
import printscript.support.parseOrNull

class OrRuleHandlerTest {
    private val grammar = grammar(
        "value",
        "value" to or("number", "id"),
        "number" to atom("NUMBER_LITERAL"),
        "id" to atom("ID")
    )

    @Test
    fun `or returns the first alternative that matches`() {
        val node = parse(grammar, Tokens.number("7"))
        assertEquals("number", node.name)
    }

    @Test
    fun `or tries the next alternative when the first fails`() {
        val node = parse(grammar, Tokens.id("x"))
        assertEquals("id", node.name)
    }

    @Test
    fun `or returns null when every alternative fails`() {
        assertNull(parseOrNull(grammar, Tokens.let()))
    }
}
