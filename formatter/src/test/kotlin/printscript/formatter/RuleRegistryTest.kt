package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.SpaceAroundOperatorRule

class RuleRegistryTest {
    @Test
    fun `no matching rule yields empty whitespace`() {
        val registry = DefaultRuleRegistry(emptyList())
        val point = FormatPoint(PointKind.BEFORE_TOKEN, tokenType = "OPERATOR")

        assertEquals("", registry.whitespaceFor(point))
    }

    @Test
    fun `combines newlines then at most one space`() {
        val extra =
            object : FormatRule {
                override fun applies(point: FormatPoint) = point.tokenType == "OPERATOR"

                override fun addChar(
                    point: FormatPoint,
                    whitespace: Char,
                ) = if (whitespace == WhitespaceChars.NEWLINE) 1 else 0
            }
        val registry = DefaultRuleRegistry(listOf(SpaceAroundOperatorRule, extra))
        val point = FormatPoint(PointKind.AFTER_TOKEN, tokenType = "OPERATOR", tokenValue = "+")

        assertEquals("\n ", registry.whitespaceFor(point))
    }
}
