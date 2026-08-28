package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.WhitespaceChars

object SpaceAroundOperatorRule : FormatRule {
    const val TYPE = "space-around-operator"

    override fun applies(point: FormatPoint): Boolean = point.tokenType == "OPERATOR"

    override fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int =
        if (whitespace == WhitespaceChars.SPACE) {
            1
        } else {
            0
        }
}
