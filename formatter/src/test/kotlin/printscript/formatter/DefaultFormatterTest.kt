package printscript.formatter

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.ast.Location
import printscript.formatter.rules.SpaceAroundOperatorRule
import printscript.formatter.support.addition
import printscript.formatter.support.expression
import printscript.formatter.support.number
import printscript.formatter.support.plus
import printscript.formatter.support.program
import printscript.formatter.support.star
import printscript.formatter.support.term
import printscript.formatter.support.token
import printscript.formatter.support.wrap
import printscript.syntax.SyntaxNode
import printscript.util.Result

class DefaultFormatterTest {
    private val withRule = DefaultFormatterFactory.create(listOf(SpaceAroundOperatorRule))
    private val withoutRules = DefaultFormatterFactory.create(emptyList())

    @Test
    fun `formats addition with spaces around the operator`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 3))

        assertEquals("1 + 2", ok(withRule.format(program)))
    }

    @Test
    fun `without rules concatenates lexemes`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 3))

        assertEquals("1+2", ok(withoutRules.format(program)))
    }

    @Test
    fun `formats nested term like the parser would`() {
        val tree =
            expression(
                wrap("term", number("1", 1)),
                plus(2),
                term(number("2", 3), star(4), number("3", 5)),
            )

        assertEquals("1 + 2 * 3", ok(withRule.format(program(tree))))
    }

    @Test
    fun `missing lexeme is a Result Err`() {
        val token = token("OPERATOR", "+", 1)
        val node =
            SyntaxNode(
                name = "OPERATOR",
                token = token.copy(value = Optional.empty()),
                location = token.location,
            )

        val result = withRule.format(program(node))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is MissingLexeme)
    }

    @Test
    fun `empty node is unrecognized`() {
        val node = SyntaxNode(name = "mystery", location = Location.empty())

        val result = withRule.format(program(node))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UnrecognizedNode)
    }

    private fun ok(result: Result<String, FormatError>): String {
        assertTrue(result is Result.Ok, "expected Ok but was $result")
        return (result as Result.Ok).value
    }
}
