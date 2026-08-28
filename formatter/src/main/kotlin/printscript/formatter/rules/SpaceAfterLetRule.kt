package printscript.formatter.rules

import printscript.formatter.FormatPoint
import printscript.formatter.PointKind
import printscript.formatter.WhitespaceChars

object SpaceAfterLetRule : FormatRule {
    const val TYPE = "space-after-let"

    override fun applies(point: FormatPoint): Boolean = point.tokenType == "LET" && point.kind == PointKind.AFTER_TOKEN

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
