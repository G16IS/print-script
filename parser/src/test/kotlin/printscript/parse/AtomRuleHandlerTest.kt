package printscript.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.parser.parse.ParseResult
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.evaluator
import printscript.support.grammar
import printscript.support.parse
import printscript.support.source

class AtomRuleHandlerTest {
    private val grammar = grammar("n", "n" to atom("NUMBER_LITERAL"))
    private val evaluator = evaluator(grammar)

    @Test
    fun `atom matches a token of the expected type`() {
        val node = parse(grammar, Tokens.number("42"))
        assertEquals("n", node.name)
        assertEquals("42", node.token!!.value.get())
    }

    @Test
    fun `atom fails without consuming the current token`() {
        val tokens = source(Tokens.of("ID", "x"))
        val result = evaluator.evaluate("n", tokens)
        assertTrue(result is ParseResult.Missing)
        assertEquals("ID", tokens.peek().type)
    }

    @Test
    fun `unknown rule name fails with an error`() {
        assertThrows<IllegalStateException> {
            evaluator.evaluate("missing", source(Tokens.number("1")))
        }
    }
}
