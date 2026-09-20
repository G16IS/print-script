package printscript.evaluator

import printscript.domain.TokenRule

interface MatchingRuleEvaluator {
    fun applies(rule: TokenRule): Boolean

    fun evaluate(
        text: String,
        rule: TokenRule,
        category: String,
    ): MatchResult

    companion object {
        fun errorMessage(
            expected: String,
            actual: TokenRule,
        ): String = "\"expected: $expected\", got \"${actual::class.simpleName}\""
    }
}
