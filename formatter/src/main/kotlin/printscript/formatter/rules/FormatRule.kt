package printscript.formatter.rules

import printscript.formatter.FormatPoint

interface FormatRule {
    fun applies(point: FormatPoint): Boolean

    fun whitespace(point: FormatPoint): String
}
