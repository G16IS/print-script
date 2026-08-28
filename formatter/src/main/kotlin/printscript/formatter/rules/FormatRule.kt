package printscript.formatter.rules

import printscript.formatter.FormatPoint

interface FormatRule {
    fun applies(point: FormatPoint): Boolean

    /**
     * How many of [whitespace] to emit at [point].
     * `0` if this rule does not use that character.
     */
    fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int
}
