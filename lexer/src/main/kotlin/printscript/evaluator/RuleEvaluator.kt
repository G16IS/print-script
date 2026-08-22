package printscript.evaluator

import printscript.domain.ExactRule
import printscript.domain.RegexRule
import printscript.domain.TokenRule

class RuleEvaluator(
    private val config: Map<String, List<TokenRule>>,
) {
    fun evaluate(currentToken: String): List<MatchResult> =
        config.flatMap { (category, rules) ->
            evaluateRuleList(currentToken, category, rules)
        }

    private fun evaluateRuleList(
        text: String,
        category: String,
        tokenRules: List<TokenRule>,
    ): List<MatchResult> = tokenRules.map { rule -> evaluateRule(text, rule, category) }

    private fun evaluateRule(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult =
        when (rule) {
            is ExactRule -> evaluateExactRule(text, rule, category)
            is RegexRule -> evaluateRegexRule(text, rule, category)
        }

    private fun evaluateExactRule(
        text: String,
        rule: ExactRule,
        category: String,
    ): MatchResult {
        val matchType =
            when {
                rule.matcher.any { it == text } -> MatchType.VALID
                text.isNotEmpty() && rule.matcher.any { it.startsWith(text) } -> MatchType.PARTIAL
                else -> MatchType.INVALID
            }
        return MatchResult(rule, matchType, category)
    }

    private fun evaluateRegexRule(
        text: String,
        rule: RegexRule,
        category: String,
    ): MatchResult {
        val matchType =
            when {
                text.isNotEmpty() && rule.matcher.any { Regex(it).matches(text) } -> MatchType.VALID
                text.isNotEmpty() && Regex(rule.partial).matches(text) -> MatchType.PARTIAL
                else -> MatchType.INVALID
            }
        return MatchResult(rule, matchType, category)
    }
}
