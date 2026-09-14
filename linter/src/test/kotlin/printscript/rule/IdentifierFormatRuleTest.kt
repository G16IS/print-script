package printscript.rule

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.domain.Token
import printscript.error.InvalidIdentifierFormat
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode

class IdentifierFormatRuleTest {
    private val idLoc =
        Location(
            start = CharPosition(1, 5),
            end = CharPosition(1, 15),
        )
    private val varLoc =
        Location(
            start = CharPosition(1, 1),
            end = CharPosition(1, 20),
        )

    private fun variableNode(identifier: String): SyntaxNode {
        val idNode =
            SyntaxNode(
                name = "ID",
                token = Token("ID", Optional.of(identifier), idLoc),
                location = idLoc,
            )
        return SyntaxNode(
            name = "variable",
            children = listOf(idNode),
            location = varLoc,
        )
    }

    @Test
    fun `supports only variable node`() {
        val rule = IdentifierFormatRule(LetterCase.CAMEL_CASE)

        assertTrue(rule.supports(SyntaxNode(name = "variable", location = varLoc)))
        assertFalse(rule.supports(SyntaxNode(name = "call", location = varLoc)))
        assertFalse(rule.supports(SyntaxNode(name = "expression-stmt", location = varLoc)))
    }

    @Test
    fun `camelCase rule accepts valid camelCase identifiers`() {
        val rule = IdentifierFormatRule(LetterCase.CAMEL_CASE)

        assertTrue(rule.check(variableNode("myVariable")).isEmpty())
        assertTrue(rule.check(variableNode("x")).isEmpty())
        assertTrue(rule.check(variableNode("count1")).isEmpty())
        assertTrue(rule.check(variableNode("camelCaseValue")).isEmpty())
    }

    @Test
    fun `camelCase rule rejects snake_case and PascalCase`() {
        val rule = IdentifierFormatRule(LetterCase.CAMEL_CASE)

        val errors = rule.check(variableNode("my_variable"))
        assertEquals(1, errors.size)

        val error = assertIs<InvalidIdentifierFormat>(errors.first())
        assertEquals("my_variable", error.identifier)
        assertEquals("camelCase", error.expectedFormat)
        assertEquals(idLoc, error.location)
    }

    @Test
    fun `snake_case rule accepts valid snake_case identifiers`() {
        val rule = IdentifierFormatRule(LetterCase.SNAKE_CASE)

        assertTrue(rule.check(variableNode("my_variable")).isEmpty())
        assertTrue(rule.check(variableNode("x")).isEmpty())
        assertTrue(rule.check(variableNode("user_first_name_123")).isEmpty())
    }

    @Test
    fun `snake_case rule rejects camelCase and SCREAMING_SNAKE`() {
        val rule = IdentifierFormatRule(LetterCase.SNAKE_CASE)

        val errors = rule.check(variableNode("myVariable"))
        assertEquals(1, errors.size)

        val error = assertIs<InvalidIdentifierFormat>(errors.first())
        assertEquals("myVariable", error.identifier)
        assertEquals("snake_case", error.expectedFormat)
        assertEquals(idLoc, error.location)
    }
}
