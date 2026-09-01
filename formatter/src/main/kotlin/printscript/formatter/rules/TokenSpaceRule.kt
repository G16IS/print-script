package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.PointKind
import printscript.formatter.WhitespaceChars

data class TokenSpaceRule(
    private val tokenType: String,
    private val kinds: Set<PointKind>,
    private val enabled: Boolean,
) : FormatRule {
    override fun applies(point: FormatPoint): Boolean = point.tokenType == tokenType && point.kind in kinds

    override fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int =
        if (enabled && whitespace == WhitespaceChars.SPACE) {
            1
        } else {
            0
        }
}
