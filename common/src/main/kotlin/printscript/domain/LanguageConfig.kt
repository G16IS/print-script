package printscript.domain

data class LanguageConfig(
    val order: List<String>,
    val config: Map<String, List<TokenRule>>,
)
