package printscript

import printscript.domain.LanguageConfig
import printscript.domain.TokenRule

data class RuleDrawResolver(
    val langConfig: LanguageConfig,
) {
    /**
     * Picks the matching rule whose category appears first in [LanguageConfig.order].
     * If several rules share that category, throws.
     */
    fun resolve(matchingRules: List<TokenRule>): TokenRule {
        require(matchingRules.isNotEmpty()) { "No matching rules provided" }

        val highestCategory = findHighestPriorityCategory(matchingRules)
        val candidates = filterByCategory(matchingRules, highestCategory)

        validateUniqueRule(candidates)
        return candidates.single()
    }

    private fun findHighestPriorityCategory(rules: List<TokenRule>): String =
        rules
            .map { findCategory(it) }
            .minByOrNull { findPriority(it) }
            ?: error("No categories found")

    private fun filterByCategory(
        rules: List<TokenRule>,
        category: String,
    ): List<TokenRule> = rules.filter { findCategory(it) == category }

    private fun validateUniqueRule(rules: List<TokenRule>) {
        require(rules.size == 1) {
            "Multiple rules found with the same priority: $rules"
        }
    }

    /** Lower index in `order` = higher priority. A category missing from `order` loses to every listed one. */
    private fun findPriority(category: String): Int {
        val index = langConfig.order.indexOf(category)
        return if (index < 0) Int.MAX_VALUE else index
    }

    private fun findCategory(rule: TokenRule): String {
        val entry =
            langConfig.config.entries
                .find { it.value.contains(rule) }
                ?: error("Rule $rule not found in config")

        return entry.key
    }
}
