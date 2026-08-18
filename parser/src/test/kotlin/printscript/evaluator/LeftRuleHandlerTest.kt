package printscript.evaluator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.error.ParseException
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.evaluator
import printscript.support.grammar
import printscript.support.left
import printscript.support.lhs
import printscript.support.op
import printscript.support.parse
import printscript.support.rhs
import printscript.support.source

class LeftRuleHandlerTest {
    private val grammar = grammar(
        "expr",
        "expr" to left("num", "OPERATOR", "+", "-"),
        "num" to atom("NUMBER_LITERAL")
    )

    @Test
    fun `left without an operator returns the child node as a wrapper`() {
        val node = parse(grammar, Tokens.number("1"))
        assertEquals("expr", node.name)
        assertEquals("num", node.children.single().name)
    }

    @Test
    fun `left associates to the left`() {
        val node = parse(
            grammar,
            Tokens.number("1"),
            Tokens.op("-"),
            Tokens.number("2"),
            Tokens.op("-"),
            Tokens.number("3")
        )
        assertEquals("-", node.op())
        assertEquals("3", node.rhs().value())
        assertEquals("-", node.lhs().op())
        assertEquals("1", node.lhs().lhs().value())
        assertEquals("2", node.lhs().rhs().value())
    }

    @Test
    fun `left does not consume an operator from a lower rule`() {
        val tokens = source(Tokens.number("1"), Tokens.op("*"), Tokens.number("2"))
        val node = evaluator(grammar).evaluate("expr", tokens)!!
        assertEquals("expr", node.name)
        assertEquals(1, node.children.size)
        assertEquals("*", tokens.peek().value.get())
    }

    @Test
    fun `left fails when the right operand is missing`() {
        assertThrows<ParseException> {
            parse(grammar, Tokens.number("1"), Tokens.op("+"))
        }
    }
}
