package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.PointKind
import printscript.formatter.WhitespaceChars

data class NewlinesBeforePrintlnRule(
    val count: Int,
) : FormatRule {
    override fun applies(point: FormatPoint): Boolean =
        point.kind == PointKind.BEFORE_TOKEN &&
            point.tokenType == "CALL" &&
            point.tokenValue == "println" &&
            point.previousTokenType == "SEMICOLON"

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
        const val TYPE = "newlines-before-println"
        val ALLOWED_COUNTS = 0..2
        const val DEFAULT_COUNT = 1
    }
}
