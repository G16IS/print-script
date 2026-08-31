package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.formatter.support.spaceAroundOperator

class TokenSpaceRuleTest {
    private val rule = spaceAroundOperator()

    @Test
    fun `applies to operator tokens`() {
        val point = FormatPoint(PointKind.BEFORE_TOKEN, tokenType = "OPERATOR", tokenValue = "+")

        assertTrue(rule.applies(point))
        assertEquals(1, rule.addChar(point, WhitespaceChars.SPACE))
        assertEquals(0, rule.addChar(point, WhitespaceChars.NEWLINE))
    }

    @Test
    fun `does not apply to number literals`() {
        val point = FormatPoint(PointKind.BEFORE_TOKEN, tokenType = "NUMBER_LITERAL", tokenValue = "1")
        assertFalse(rule.applies(point))
    }
}
