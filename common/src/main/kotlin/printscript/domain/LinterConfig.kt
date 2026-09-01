package printscript.domain

data class LinterConfig(
    val rules: Map<String, RuleConfig> = emptyMap(),
) {
    fun enabled(): Map<String, RuleConfig> = rules.filterValues { it.enabled }
}

data class RuleConfig(
    val enabled: Boolean,
    val options: Map<String, String> = emptyMap(),
)
