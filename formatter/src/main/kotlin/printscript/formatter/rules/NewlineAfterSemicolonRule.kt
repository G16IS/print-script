package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.PointKind
import printscript.formatter.WhitespaceChars

object NewlineAfterSemicolonRule : FormatRule {
    const val TYPE = "newline-after-semicolon"

    override fun applies(point: FormatPoint): Boolean =
        point.tokenType == "SEMICOLON" && point.kind == PointKind.AFTER_TOKEN

    override fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int =
        if (whitespace == WhitespaceChars.NEWLINE) {
            1
        } else {
            0
        }
}
