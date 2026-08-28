package printscript.formatter.rules

import printscript.formatter.FormatPoint

object SpaceAroundOperatorRule : FormatRule {
    const val TYPE = "space-around-operator"

    override fun applies(point: FormatPoint): Boolean = point.tokenType == "OPERATOR"

    override fun whitespace(point: FormatPoint): String = " "
}
