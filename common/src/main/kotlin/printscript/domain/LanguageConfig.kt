package printscript.domain

class LanguageConfig(
    private val order: List<String>,
    private val rulesByCategory: Map<String, List<TokenRule>>,
) {
    fun rulesInOrder(): List<Pair<String, List<TokenRule>>> =
        order.map { category -> category to rulesOf(category) }

    fun categoryOf(rule: TokenRule): String? = rulesByCategory.entries
        .find { it.value.contains(rule) }?.key

    fun findPriority(category: String): Int {
        val index = order.indexOf(category)
        return if (index < 0) Int.MAX_VALUE else index
    }

    private fun rulesOf(category: String): List<TokenRule> {
        assert(category in order) { "Category '$category' not found in language configuration." }
        return rulesByCategory[category] ?: emptyList()
    }
}
