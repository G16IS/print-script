package printscript.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.parser.parse.ParseResult
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.capture
import printscript.support.evaluator
import printscript.support.expect
import printscript.support.grammar
import printscript.support.optional
import printscript.support.parse
import printscript.support.ref
import printscript.support.seq
import printscript.support.source

class OptionalRuleHandlerTest {
    private val optId =
        grammar(
            "opt",
            "opt" to optional("id"),
            "id" to atom("ID"),
        )

    @Test
    fun `absent inner produces a node with zero children`() {
        val node = parse(optId, Tokens.semicolon())
        assertEquals("opt", node.name)
        assertEquals(0, node.children.size)
    }

    @Test
    fun `present inner produces a node with one child`() {
        val node = parse(optId, Tokens.id("x"))
        assertEquals("opt", node.name)
        assertEquals(1, node.children.size)
        assertEquals("x", node.children[0].value())
    }

    @Test
    fun `inner failure propagates as Failed`() {
        val g =
            grammar(
                "opt",
                "opt" to optional("pair"),
                "pair" to seq(expect("ELSE"), capture("ID")),
            )
        val tokens = source(Tokens.of("ELSE"), Tokens.semicolon())
        val result = evaluator(g).evaluate("opt", tokens)
        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `if grammar with optional else`() {
        val g =
            grammar(
                "if",
                "if" to
                    seq(
                        expect("IF"),
                        expect("LEFT_PAREN"),
                        ref("id"),
                        expect("RIGHT_PAREN"),
                        expect("LEFT_BRACE"),
                        expect("RIGHT_BRACE"),
                        ref("else-opt"),
                    ),
                "else-opt" to optional("else-clause"),
                "else-clause" to seq(expect("ELSE"), expect("LEFT_BRACE"), expect("RIGHT_BRACE")),
                "id" to atom("ID"),
            )

        // without else
        val noElse =
            parse(
                g,
                Tokens.of("IF"),
                Tokens.lparen(),
                Tokens.id("x"),
                Tokens.rparen(),
                Tokens.lbrace(),
                Tokens.rbrace(),
            )
        val elseOpt1 = noElse.children.last()
        assertEquals("else-opt", elseOpt1.name)
        assertEquals(0, elseOpt1.children.size)

        // with else
        val withElse =
            parse(
                g,
                Tokens.of("IF"),
                Tokens.lparen(),
                Tokens.id("y"),
                Tokens.rparen(),
                Tokens.lbrace(),
                Tokens.rbrace(),
                Tokens.of("ELSE"),
                Tokens.lbrace(),
                Tokens.rbrace(),
            )
        val elseOpt2 = withElse.children.last()
        assertEquals("else-opt", elseOpt2.name)
        assertEquals(1, elseOpt2.children.size)
    }
}
