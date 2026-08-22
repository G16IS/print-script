package printscript.domain

data class Grammar(
    val start: String,
    val rules: Map<String, GrammarRule>,
) {
    init {
        validate()
    }

    fun rule(name: String): GrammarRule = rules[name] ?: error("Unknown grammar rule: $name")

    private fun validate() {
        checkStart()
        checkReferences()
    }

    private fun checkStart() {
        require(start in rules) { "Unknown start rule: $start" }
    }

    private fun checkReferences() {
        val missing = missingReferences()
        require(missing.isEmpty()) { "Unknown rule references: $missing" }
    }

    private fun missingReferences(): List<String> =
        rules.values
            .flatMap { it.references() }
            .filter { it !in rules }
            .distinct()
}
