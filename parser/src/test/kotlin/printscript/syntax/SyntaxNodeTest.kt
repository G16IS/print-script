package printscript.syntax

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.grammar
import printscript.support.left
import printscript.support.parse

class SyntaxNodeTest {
    @Test
    fun `child and value read a captured token`() {
        val node =
            parse(
                grammar("n", "n" to atom("ID")),
                Tokens.id("x"),
            )
        assertEquals("x", node.value())
        assertNull(node.childOrNull("missing"))
    }

    @Test
    fun `find walks through wrapper rules`() {
        val g =
            grammar(
                "expr",
                "expr" to left("num", "OPERATOR", "+"),
                "num" to atom("NUMBER_LITERAL"),
            )
        val node = parse(g, Tokens.number("5"))
        assertEquals("expr", node.name)
        assertEquals("5", node.find("num").value())
    }
}
