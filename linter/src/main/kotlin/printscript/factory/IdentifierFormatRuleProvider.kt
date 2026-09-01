package printscript.factory

import printscript.rule.IdentifierFormatRule
import printscript.rule.LetterCase
import printscript.rule.LintRule

class IdentifierFormatRuleProvider : LintRuleProvider {
    override val id: String = "identifier-format"

    override fun create(options: Map<String, String>): LintRule {
        val formatStr = options["format"] ?: "camelCase"
        val letterCase =
            when (formatStr) {
                "camelCase" -> LetterCase.CAMEL_CASE
                "snake_case" -> LetterCase.SNAKE_CASE
                else ->
                    throw IllegalArgumentException(
                        "Unknown identifier format: '$formatStr'. Expected 'camelCase' or 'snake_case'",
                    )
            }
        return IdentifierFormatRule(letterCase)
    }
}
