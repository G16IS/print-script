package printscript.evaluator

import printscript.domain.ExactRule
import printscript.domain.TokenRule

class ExactEvaluator : MatchingRuleEvaluator {
    override fun applies(rule: TokenRule): Boolean = rule is ExactRule

    override fun evaluate(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult {
        assert(applies(rule)) { MatchingRuleEvaluator.errorMessage("ExactRule", rule) }

        val matchType =
            MatchType.of(
                exact = { rule.matcher.any { it == text } },
                partial = { text.isNotEmpty() && rule.matcher.any { it.startsWith(text) } },
            )

        return MatchResult(rule, matchType, category)
    }
}
