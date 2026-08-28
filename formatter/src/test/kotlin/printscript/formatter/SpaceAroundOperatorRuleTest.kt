package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.formatter.rules.SpaceAroundOperatorRule

class SpaceAroundOperatorRuleTest {
    @Test
    fun `applies to operator tokens`() {
        val point = FormatPoint(PointKind.BEFORE_TOKEN, tokenType = "OPERATOR", tokenValue = "+")
        assertTrue(SpaceAroundOperatorRule.applies(point))
        assertEquals(" ", SpaceAroundOperatorRule.whitespace(point))
    }

    @Test
    fun `does not apply to number literals`() {
        val point = FormatPoint(PointKind.BEFORE_TOKEN, tokenType = "NUMBER_LITERAL", tokenValue = "1")
        assertFalse(SpaceAroundOperatorRule.applies(point))
    }
}
