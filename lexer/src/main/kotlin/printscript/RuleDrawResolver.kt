package printscript

import printscript.domain.LanguageConfig
import printscript.domain.TokenRule
import printscript.error.LexerError
import printscript.error.MultipleRulesWithSamePriority
import printscript.error.NoRulesProvided
import printscript.error.RuleNotFound
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

data class RuleDrawResolver(
    val langConfig: LanguageConfig,
) {
    /**
     * Picks the matching rule whose category appears first in [LanguageConfig.order].
     * If several rules share that category, returns an error.
     */
    fun resolve(matchingRules: List<TokenRule>): Result<TokenRule, LexerError> {
        if (matchingRules.isEmpty()) return Result.Err(NoRulesProvided())

        return categorize(matchingRules).flatMap { selectWinner(it) }
    }

    private fun categorize(rules: List<TokenRule>): Result<List<Pair<String, TokenRule>>, LexerError> =
        rules.fold(Result.Ok(emptyList())) { acc, rule ->
            acc.flatMap { pairs -> findCategory(rule).map { pairs + (it to rule) } }
        }

    private fun selectWinner(pairs: List<Pair<String, TokenRule>>): Result<TokenRule, LexerError> {
        val winners = topCategory(pairs)
        if (winners.size == 1) return Result.Ok(winners.single())
        return Result.Err(MultipleRulesWithSamePriority(rules = winners))
    }

    private fun topCategory(pairs: List<Pair<String, TokenRule>>): List<TokenRule> {
        val bestCategory = pairs.minByOrNull { findPriority(it.first) }?.first ?: return emptyList()
        return pairs.filter { it.first == bestCategory }.map { it.second }
    }

    /** Lower index in `order` = higher priority. A category missing from `order` loses to every listed one. */
    private fun findPriority(category: String): Int {
        val index = langConfig.order.indexOf(category)
        return if (index < 0) Int.MAX_VALUE else index
    }

    private fun findCategory(rule: TokenRule): Result<String, LexerError> {
        val entry =
            langConfig.config.entries
                .find { it.value.contains(rule) }
                ?: return Result.Err(RuleNotFound(rule = rule))

        return Result.Ok(entry.key)
    }
}
