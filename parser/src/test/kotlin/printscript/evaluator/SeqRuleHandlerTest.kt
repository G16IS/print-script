package printscript.evaluator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.error.ParseException
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.capture
import printscript.support.evaluator
import printscript.support.expect
import printscript.support.grammar
import printscript.support.parse
import printscript.support.ref
import printscript.support.seq
import printscript.support.source

class SeqRuleHandlerTest {
    @Test
    fun `seq keeps captured tokens and nested rules`() {
        val g = grammar(
            "s",
            "s" to seq(expect("LET"), capture("ID"), ref("n")),
            "n" to atom("NUMBER_LITERAL")
        )
        val node = parse(g, Tokens.let(), Tokens.id("x"), Tokens.number("1"))
        assertEquals("s", node.name)
        assertEquals("x", node.children[0].value())
        assertEquals("n", node.children[1].name)
    }

    @Test
    fun `seq ignores tokens that are not captured`() {
        val g = grammar("s", "s" to seq(expect("LET"), expect("SEMICOLON")))
        val node = parse(g, Tokens.let(), Tokens.semicolon())
        assertEquals(emptyList<String>(), node.children.map { it.name })
    }

    @Test
    fun `seq that fails on the first step restores the cursor`() {
        val g = grammar("s", "s" to seq(expect("LET"), capture("ID")))
        val tokens = source(Tokens.number("1"), Tokens.id("x"))
        assertNull(evaluator(g).evaluate("s", tokens))
        assertEquals("NUMBER_LITERAL", tokens.peek().type)
    }

    @Test
    fun `seq that fails halfway throws`() {
        val g = grammar("s", "s" to seq(expect("LET"), expect("SEMICOLON")))
        assertThrows<ParseException> {
            parse(g, Tokens.let(), Tokens.id("x"))
        }
    }
}
