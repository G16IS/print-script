package printscript.formatter.rules

import printscript.formatter.FormatPoint

/**
 * Language-fixed cap: the registry never emits more than one space.
 * This rule does not request whitespace of its own.
 */
object MaxOneSpaceRule : FormatRule {
    const val TYPE = "max-one-space"

    override fun applies(point: FormatPoint): Boolean = true

    override fun addChar(
        point: FormatPoint,
        whitespace: Char,
    ): Int = 0
}
