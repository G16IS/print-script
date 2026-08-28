package printscript.formatter

import printscript.formatter.rules.FormatRule

interface RuleRegistry {
    fun whitespaceFor(point: FormatPoint): String
}

class DefaultRuleRegistry(
    private val rules: List<FormatRule>,
) : RuleRegistry {
    override fun whitespaceFor(point: FormatPoint): String {
        val applicable = rules.filter { it.applies(point) }

        if (applicable.isEmpty()) {
            return ""
        }

        val newlines =
            applicable
                .maxOf { it.addChar(point, WhitespaceChars.NEWLINE) }
                .coerceAtLeast(0)

        val spaces =
            applicable
                .maxOf { it.addChar(point, WhitespaceChars.SPACE) }
                .coerceIn(0, 1)

        return WhitespaceChars.NEWLINE.toString().repeat(newlines) +
            WhitespaceChars.SPACE.toString().repeat(spaces)
    }
}
