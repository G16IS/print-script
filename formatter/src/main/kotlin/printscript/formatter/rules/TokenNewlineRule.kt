package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.PointKind
import printscript.formatter.WhitespaceChars

data class TokenNewlineRule(
    private val tokenType: String,
    private val kinds: Set<PointKind>,
    private val count: Int,
    private val tokenValue: String? = null,
    private val previousTokenType: String? = null,
) : FormatRule {
    override fun applies(point: FormatPoint): Boolean =
        point.tokenType == tokenType &&
            point.kind in kinds &&
            (tokenValue == null || point.tokenValue == tokenValue) &&
            (previousTokenType == null || point.previousTokenType == previousTokenType)

    override fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int =
        if (whitespace == WhitespaceChars.NEWLINE) {
            count
        } else {
            0
        }

    companion object {
        val ALLOWED_COUNTS = 0..2
        const val DEFAULT_COUNT = 1
    }
}
