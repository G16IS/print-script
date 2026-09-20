package printscript.support

import printscript.domain.TokenRule
import printscript.evaluator.MatchResult
import printscript.evaluator.MatchType
import printscript.evaluator.MatchingRuleEvaluator

class MockMatchingRuleEvaluator(
    private val canApply: (TokenRule) -> Boolean = { true },
    private val onEvaluate: (text: String, rule: TokenRule, category: String) -> MatchResult = { _, rule, cat ->
        MatchResult(rule, MatchType.VALID, cat)
    },
) : MatchingRuleEvaluator {
    override fun applies(rule: TokenRule): Boolean = canApply(rule)

    override fun evaluate(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult = onEvaluate(text, rule, category)
}
