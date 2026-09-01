package printscript.domain

/**
 * Maps token types to their fixed lexeme values.
 * Only works for ExactRule tokens with a single matcher value
 * (e.g., LET → "let", SEMICOLON → ";").
 * Tokens with multiple matchers (e.g., OPERATOR → ["+", "-", "*", "/"])
 * are excluded because they are always captured from the source.
 */
data class TokenLexemes(
    private val lexemes: Map<String, String>,
) {
    fun lexemeFor(tokenType: String): String? = lexemes[tokenType]

    companion object {
        fun from(config: LanguageConfig): TokenLexemes {
            val lexemes = mutableMapOf<String, String>()
            for (rules in config.config.values) {
                for (rule in rules) {
                    if (rule is ExactRule && rule.matcher.size == 1) {
                        lexemes[rule.token] = rule.matcher.single()
                    }
                }
            }
            return TokenLexemes(lexemes)
        }
    }
}
