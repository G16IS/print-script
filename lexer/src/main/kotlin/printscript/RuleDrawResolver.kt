package printscript

import printscript.domain.LanguageConfig
import printscript.domain.TokenRule

data class RuleDrawResolver(
    val langConfig: LanguageConfig,
) {
    /**
     * Resolve multiple rule matchings
     *
     * @param config Language configuration
     * @param matchingRules List of matching rules
     * @return The rule with the highest priority
     *
     * Algorithm:
     * - iterate over matchingRules
     * - for each rule, check priority
     * - if rule has the most priority, save it
     * - return saved rule
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
            .maxByOrNull { findPriority(it) }
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

    private fun findPriority(category: String): Int = langConfig.order.indexOf(category)

    private fun findCategory(rule: TokenRule): String {
        val entry =
            langConfig.config.entries
                .find { it.value.contains(rule) }
                ?: error("Rule $rule not found in config")

        return entry.key
    }
}
