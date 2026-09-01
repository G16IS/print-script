package printscript.domain

data class FormatterRulesConfig(
    val rules: List<FormatRuleSpec> = emptyList(),
)

data class FormatRuleSpec(
    val type: String,
    val enabled: Boolean? = null,
    val count: Int? = null,
)
