package printscript.rule

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.ast.Location
import printscript.domain.Token
import printscript.error.InvalidPrintlnArgument
import printscript.reader.CharPosition
import printscript.syntax.SyntaxNode

class PrintlnArgumentRuleTest {
    private val loc =
        Location(
            start = CharPosition(1, 1),
            end = CharPosition(1, 15),
        )

    private fun callNode(
        callee: String,
        expressionNode: SyntaxNode,
    ): SyntaxNode {
        val callToken =
            SyntaxNode(
                name = "CALL",
                token = Token("CALL", Optional.of(callee), loc),
                location = loc,
            )
        return SyntaxNode(
            name = "call",
            children = listOf(callToken, expressionNode),
            location = loc,
        )
    }

    private fun simpleExpr(
        leafName: String,
        leafValue: String,
    ): SyntaxNode {
        val leaf =
            SyntaxNode(
                name = leafName,
                token = Token(leafName.uppercase(), Optional.of(leafValue), loc),
                location = loc,
            )
        val term = SyntaxNode(name = "term", children = listOf(leaf), location = loc)
        return SyntaxNode(name = "expression", children = listOf(term), location = loc)
    }

    @Test
    fun `supports matches println call and rejects other callees or nodes`() {
        val rule = PrintlnArgumentRule()
        val printlnCall = callNode("println", simpleExpr("identifier", "x"))
        val otherCall = callNode("foo", simpleExpr("identifier", "x"))
        val variableNode = SyntaxNode(name = "variable", location = loc)

        assertTrue(rule.supports(printlnCall))
        assertFalse(rule.supports(otherCall))
        assertFalse(rule.supports(variableNode))
    }

    @Test
    fun `accepts identifier argument`() {
        val rule = PrintlnArgumentRule()
        val node = callNode("println", simpleExpr("identifier", "myVar"))

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `accepts number literal argument`() {
        val rule = PrintlnArgumentRule()
        val node = callNode("println", simpleExpr("number", "42"))

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `accepts string literal argument`() {
        val rule = PrintlnArgumentRule()
        val node = callNode("println", simpleExpr("string", "hello"))

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `accepts grouped identifier argument`() {
        val rule = PrintlnArgumentRule()
        val innerLeaf =
            SyntaxNode(
                name = "identifier",
                token = Token("ID", Optional.of("x"), loc),
                location = loc,
            )
        val innerExpr =
            SyntaxNode(
                name = "expression",
                children = listOf(SyntaxNode(name = "term", children = listOf(innerLeaf), location = loc)),
                location = loc,
            )
        val group =
            SyntaxNode(
                name = "group",
                children =
                    listOf(
                        SyntaxNode(name = "LEFT_PAREN", location = loc),
                        innerExpr,
                        SyntaxNode(name = "RIGHT_PAREN", location = loc),
                    ),
                location = loc,
            )
        val outerExpr =
            SyntaxNode(
                name = "expression",
                children = listOf(SyntaxNode(name = "term", children = listOf(group), location = loc)),
                location = loc,
            )

        val node = callNode("println", outerExpr)
        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `rejects binary expression argument`() {
        val rule = PrintlnArgumentRule()
        val leftTerm =
            SyntaxNode(
                name = "term",
                children =
                    listOf(
                        SyntaxNode(name = "number", token = Token("NUMBER", Optional.of("1"), loc), location = loc),
                    ),
                location = loc,
            )
        val op = SyntaxNode(name = "OPERATOR", token = Token("OPERATOR", Optional.of("+"), loc), location = loc)
        val rightTerm =
            SyntaxNode(
                name = "term",
                children =
                    listOf(
                        SyntaxNode(name = "number", token = Token("NUMBER", Optional.of("2"), loc), location = loc),
                    ),
                location = loc,
            )
        val binaryExpr = SyntaxNode(name = "expression", children = listOf(leftTerm, op, rightTerm), location = loc)

        val node = callNode("println", binaryExpr)
        val errors = rule.check(node)

        assertEquals(1, errors.size)
        val error = assertIs<InvalidPrintlnArgument>(errors.first())
        assertEquals(loc, error.location)
    }

    @Test
    fun `rejects nested call expression argument`() {
        val rule = PrintlnArgumentRule()
        val nestedCall = callNode("foo", simpleExpr("identifier", "x"))
        val expr =
            SyntaxNode(
                name = "expression",
                children = listOf(SyntaxNode(name = "term", children = listOf(nestedCall), location = loc)),
                location = loc,
            )

        val node = callNode("println", expr)
        val errors = rule.check(node)

        assertEquals(1, errors.size)
        assertIs<InvalidPrintlnArgument>(errors.first())
    }
}
