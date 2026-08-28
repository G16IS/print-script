package printscript.formatter

import printscript.formatter.rules.FormatRule

interface RuleRegistry {
    fun whitespaceFor(point: FormatPoint): String
}

class DefaultRuleRegistry(
    private val rules: List<FormatRule>,
) : RuleRegistry {
    override fun whitespaceFor(point: FormatPoint): String =
        rules
            .filter { it.applies(point) }
            .joinToString(separator = "") { it.whitespace(point) }
}
