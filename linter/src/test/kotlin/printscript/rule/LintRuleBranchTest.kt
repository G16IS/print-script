package printscript.rule

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import printscript.domain.Token
import printscript.factory.IdentifierFormatRuleProvider
import printscript.factory.PrintlnArgumentRuleProvider
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

class LintRuleBranchTest {
    private val loc = Location(CharPosition(1, 1), CharPosition(1, 8))

    @Test
    fun `identifier format ignores a variable without an ID child`() {
        val rule = IdentifierFormatRule(LetterCase.CAMEL_CASE)
        val node = SyntaxNode(name = "variable", location = loc)

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `identifier format ignores an ID without a token object`() {
        val rule = IdentifierFormatRule(LetterCase.CAMEL_CASE)
        val id = SyntaxNode(name = "ID", location = loc)
        val node = SyntaxNode(name = "variable", children = listOf(id), location = loc)

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `identifier format ignores an ID without a token value`() {
        val rule = IdentifierFormatRule(LetterCase.SNAKE_CASE)
        val id = SyntaxNode(name = "ID", token = Token("ID", Optional.empty(), loc), location = loc)
        val node = SyntaxNode(name = "variable", children = listOf(id), location = loc)

        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `identifier format provider defaults to camelCase`() {
        val rule = IdentifierFormatRuleProvider().create(emptyMap()) as IdentifierFormatRule

        assertEquals(LetterCase.CAMEL_CASE, rule.letterCase)
        assertTrue(rule.check(variable("camelCase")).isEmpty())
        assertFalse(rule.check(variable("snake_case")).isEmpty())
    }

    @Test
    fun `println does not support a call without a CALL token`() {
        val rule = PrintlnArgumentRule()
        val node = SyntaxNode(name = "call", location = loc)

        assertFalse(rule.supports(node))
        assertTrue(rule.check(node).isEmpty())
    }

    @Test
    fun `println does not support a CALL child without a token`() {
        val rule = PrintlnArgumentRule()
        val call =
            SyntaxNode(
                name = "call",
                children = listOf(SyntaxNode(name = "CALL", location = loc)),
                location = loc,
            )

        assertFalse(rule.supports(call))
    }

    @Test
    fun `println does not support a CALL without a value`() {
        val rule = PrintlnArgumentRule()
        val call =
            SyntaxNode(
                name = "call",
                children =
                    listOf(
                        SyntaxNode(
                            name = "CALL",
                            token = Token("CALL", Optional.empty(), loc),
                            location = loc,
                        ),
                    ),
                location = loc,
            )

        assertFalse(rule.supports(call))
    }

    @Test
    fun `println unwraps a group whose only child is not named expression`() {
        val rule = PrintlnArgumentRule()
        val identifier =
            SyntaxNode(name = "identifier", token = Token("ID", Optional.of("x"), loc), location = loc)
        val group = SyntaxNode(name = "group", children = listOf(identifier), location = loc)
        val expr = SyntaxNode(name = "expression", children = listOf(group), location = loc)

        assertTrue(rule.check(call("println", expr)).isEmpty())
    }

    @Test
    fun `println provider uses a custom callee`() {
        val rule = PrintlnArgumentRuleProvider().create(mapOf("callee" to "print")) as PrintlnArgumentRule
        val printCall = call("print", simpleIdentifier("x"))
        val printlnCall = call("println", simpleIdentifier("x"))

        assertEquals("print", rule.callee)
        assertTrue(rule.supports(printCall))
        assertFalse(rule.supports(printlnCall))
    }

    @Test
    fun `println provider defaults the callee to println`() {
        val rule = PrintlnArgumentRuleProvider().create(emptyMap()) as PrintlnArgumentRule

        assertEquals("println", rule.callee)
        assertTrue(rule.supports(call("println", simpleIdentifier("x"))))
    }

    private fun variable(identifier: String): SyntaxNode {
        val id = SyntaxNode(name = "ID", token = Token("ID", Optional.of(identifier), loc), location = loc)
        return SyntaxNode(name = "variable", children = listOf(id), location = loc)
    }

    private fun call(
        callee: String,
        expression: SyntaxNode,
    ): SyntaxNode {
        val callToken = SyntaxNode(name = "CALL", token = Token("CALL", Optional.of(callee), loc), location = loc)
        return SyntaxNode(name = "call", children = listOf(callToken, expression), location = loc)
    }

    private fun simpleIdentifier(name: String): SyntaxNode {
        val leaf = SyntaxNode(name = "identifier", token = Token("ID", Optional.of(name), loc), location = loc)
        val term = SyntaxNode(name = "term", children = listOf(leaf), location = loc)
        return SyntaxNode(name = "expression", children = listOf(term), location = loc)
    }
}
