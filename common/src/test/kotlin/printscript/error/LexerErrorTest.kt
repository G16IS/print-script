package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.domain.ExactRule
import printscript.support.loc
import printscript.syntax.Location

class LexerErrorTest {
    private val location = loc(3, 4, 3, 5)

    @Test
    fun `UnexpectedToken includes line and column`() {
        val error = UnexpectedToken(location)

        assertEquals("Unexpected token at line 3 col 4", error.message)
    }

    @Test
    fun `UnexpectedEnfOfLine names the token that was being read`() {
        val error = UnexpectedEnfOfLine(location, wasReadingToken = "STRING_LITERAL")

        assertEquals(
            "Unexpected end of file at line 3 col 4 while reading token STRING_LITERAL",
            error.message,
        )
    }

    @Test
    fun `MultipleRulesWithSamePriority lists the rules and defaults location to origin`() {
        val rule = ExactRule(listOf("let"), "LET", capture = false)
        val error = MultipleRulesWithSamePriority(rules = listOf(rule))

        assertEquals(Location.empty(), error.location)
        assertEquals("Multiple rules found with the same priority: [$rule]", error.message)
    }

    @Test
    fun `NoRulesProvided has a fixed message and origin location`() {
        val error = NoRulesProvided()

        assertEquals(Location.empty(), error.location)
        assertEquals("No matching rules provided", error.message)
    }

    @Test
    fun `RuleNotFound names the rule`() {
        val rule = ExactRule(listOf("let"), "LET", capture = false)
        val error = RuleNotFound(rule = rule)

        assertEquals("Rule $rule not found in config", error.message)
        assertEquals(Location.empty(), error.location)
    }
}
