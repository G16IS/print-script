package printscript.formatter.factories

data class ResolvedFormatRule(
    val type: String,
    val token: String,
    val value: String? = null,
    val previous: String? = null,
    val enabled: Boolean? = null,
    val count: Int? = null,
)
