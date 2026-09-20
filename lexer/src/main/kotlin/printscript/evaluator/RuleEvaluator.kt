package printscript.evaluator

import printscript.domain.LanguageConfig
import printscript.domain.TokenRule

class RuleEvaluator(
    private val config: LanguageConfig,
    private val evaluators: List<MatchingRuleEvaluator>,
) {
    fun evaluate(currentToken: String): List<MatchResult> =
        config.rulesInOrder().flatMap { (category, rules) ->
            rules.map { rule -> evaluateRule(currentToken, rule, category) }
        }

    private fun evaluateRule(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult =
        evaluators
            .firstOrNull { it.applies(rule) }
            ?.evaluate(text, rule, category)
            ?: throw IllegalArgumentException("No evaluator found for rule: $rule")
}
