package printscript

import printscript.domain.LanguageConfig
import printscript.domain.TokenRule
import printscript.error.LexerError
import printscript.error.MultipleRulesWithSamePriority
import printscript.error.NoRulesProvided
import printscript.error.RuleNotFound
import printscript.util.Result
import printscript.util.err
import printscript.util.flatMap
import printscript.util.map
import printscript.util.ok

data class RuleDrawResolver(
    val langConfig: LanguageConfig,
) {
    /**
     * Picks the matching rule whose category appears first in [LanguageConfig.order].
     * If several rules share that category, returns an error.
     */
    fun resolve(matchingRules: List<TokenRule>): Result<TokenRule, LexerError> {
        if (matchingRules.isEmpty()) return err(NoRulesProvided())

        return categorize(matchingRules).flatMap { selectWinner(it) }
    }

    private fun categorize(rules: List<TokenRule>): Result<List<Pair<String, TokenRule>>, LexerError> =
        rules.fold(ok(emptyList())) { acc, rule ->
            acc.flatMap { pairs -> findCategory(rule).map { pairs + (it to rule) } }
        }

    private fun selectWinner(pairs: List<Pair<String, TokenRule>>): Result<TokenRule, LexerError> {
        val winners = topCategory(pairs)

        if (winners.size == 1) return ok(winners.single())

        return err(MultipleRulesWithSamePriority(rules = winners))
    }

    private fun topCategory(pairs: List<Pair<String, TokenRule>>): List<TokenRule> {
        val bestCategory =
            pairs
                .minByOrNull { langConfig.findPriority(it.first) }
                ?.first
                ?: return emptyList()

        return pairs
            .filter { it.first == bestCategory }
            .map { it.second }
    }

    private fun findCategory(rule: TokenRule): Result<String, LexerError> {
        val entry =
            langConfig.categoryOf(rule)
                ?: return err(RuleNotFound(rule = rule))

        return ok(entry)
    }
}
