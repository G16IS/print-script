package printscript.evaluator

import printscript.domain.RegexRule
import printscript.domain.TokenRule

class RegexEvaluator : MatchingRuleEvaluator {
    override fun applies(rule: TokenRule): Boolean = rule is RegexRule

    override fun evaluate(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult {
        assert(applies(rule)) { MatchingRuleEvaluator.errorMessage("RegexRule", rule) }
        val regexRule = rule as RegexRule

        val matchType =
            MatchType.of(
                exact = { text.isNotEmpty() && regexRule.matcher.any { Regex(it).matches(text) } },
                partial = { text.isNotEmpty() && Regex(regexRule.partial).matches(text) },
            )

        return MatchResult(regexRule, matchType, category)
    }
}
