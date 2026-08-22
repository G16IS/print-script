package printscript.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.capture
import printscript.support.expect
import printscript.support.grammar
import printscript.support.parse
import printscript.support.ref
import printscript.support.repeat
import printscript.support.seq

class RepeatRuleHandlerTest {
    private val items =
        grammar(
            "items",
            "items" to repeat("id"),
            "id" to atom("ID"),
        )

    @Test
    fun `repeat of zero items succeeds and does not consume`() {
        val node = parse(items, Tokens.semicolon())
        assertEquals("items", node.name)
        assertEquals(0, node.children.size)
    }

    @Test
    fun `repeat collects every consecutive match`() {
        val node = parse(items, Tokens.id("a"), Tokens.id("b"), Tokens.id("c"))
        assertEquals(listOf("a", "b", "c"), node.children.map { it.value() })
    }

    @Test
    fun `repeat inside a block parses nested statements`() {
        val g = blockGrammar()
        val node =
            parse(
                g,
                Tokens.lbrace(),
                Tokens.id("a"),
                Tokens.semicolon(),
                Tokens.id("b"),
                Tokens.semicolon(),
                Tokens.rbrace(),
            )
        val stmts = node.children.single()
        assertEquals("stmts", stmts.name)
        assertEquals(listOf("a", "b"), stmts.children.map { it.children[0].value() })
    }

    private fun blockGrammar() =
        grammar(
            "block",
            "block" to seq(expect("LEFT_BRACE"), ref("stmts"), expect("RIGHT_BRACE")),
            "stmts" to repeat("stmt"),
            "stmt" to seq(capture("ID"), expect("SEMICOLON")),
        )
}
