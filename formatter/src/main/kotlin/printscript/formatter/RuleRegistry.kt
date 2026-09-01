package printscript.formatter

import printscript.formatter.rules.FormatRule

internal interface RuleRegistry {
    fun whitespaceFor(point: FormatPoint): Gap
}

internal class DefaultRuleRegistry(
    private val rules: List<FormatRule>,
) : RuleRegistry {
    override fun whitespaceFor(point: FormatPoint): Gap {
        val applicable = rules.filter { it.applies(point) }

        if (applicable.isEmpty()) {
            return Gap.EMPTY
        }

        val newlines =
            applicable
                .maxOf { it.addChar(point, WhitespaceChars.NEWLINE) }
                .coerceAtLeast(0)

        val spaces =
            applicable
                .maxOf { it.addChar(point, WhitespaceChars.SPACE) }
                .coerceIn(0, 1)

        return Gap(newlines = newlines, spaces = spaces)
    }
}
