package printscript.domain

sealed interface TokenRule {
    val matcher: List<String>
    val token: String
    val capture: Boolean
}

data class ExactRule(
    override val matcher: List<String>,
    override val token: String,
    override val capture: Boolean,
) : TokenRule

data class RegexRule(
    override val matcher: List<String>,
    override val token: String,
    override val capture: Boolean,
    val partial: String,
) : TokenRule
