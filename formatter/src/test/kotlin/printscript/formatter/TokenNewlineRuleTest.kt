package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.formatter.rules.TokenNewlineRule

class TokenNewlineRuleTest {
    private val printlnNewlines =
        TokenNewlineRule(
            tokenType = "CALL",
            kinds = setOf(PointKind.BEFORE_TOKEN),
            count = 1,
            tokenValue = "println",
            previousTokenType = "SEMICOLON",
        )

    @Test
    fun `applies before println when previous token is semicolon`() {
        val point =
            FormatPoint(
                kind = PointKind.BEFORE_TOKEN,
                tokenType = "CALL",
                tokenValue = "println",
                previousTokenType = "SEMICOLON",
            )

        assertTrue(printlnNewlines.applies(point))
        assertEquals(1, printlnNewlines.addChar(point, WhitespaceChars.NEWLINE))
        assertEquals(0, printlnNewlines.addChar(point, WhitespaceChars.SPACE))
    }

    @Test
    fun `does not apply to other callees`() {
        val point =
            FormatPoint(
                kind = PointKind.BEFORE_TOKEN,
                tokenType = "CALL",
                tokenValue = "foo",
                previousTokenType = "SEMICOLON",
            )

        assertFalse(printlnNewlines.applies(point))
    }
}
