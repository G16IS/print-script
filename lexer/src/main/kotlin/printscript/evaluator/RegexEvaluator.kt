package printscript.evaluator

import printscript.domain.RegexRule
import printscript.domain.TokenRule

class RegexEvaluator(
    private val compile: (String) -> Regex = ::Regex,
) : MatchingRuleEvaluator {
    private val compiled = HashMap<String, Regex>()

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
                exact = { text.isNotEmpty() && regexRule.matcher.any { regex(it).matches(text) } },
                partial = { text.isNotEmpty() && regex(regexRule.partial).matches(text) },
            )

        return MatchResult(regexRule, matchType, category)
    }

    private fun regex(pattern: String): Regex = compiled.getOrPut(pattern) { compile(pattern) }
}
