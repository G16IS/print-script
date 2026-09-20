package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenLexemesTest {
    @Test
    fun `maps ExactRule tokens that have a single matcher`() {
        val config =
            LanguageConfig(
                order = listOf("keywords"),
                rulesByCategory =
                    mapOf(
                        "keywords" to
                            listOf(
                                ExactRule(listOf("let"), "LET", capture = false),
                                ExactRule(listOf(";"), "SEMICOLON", capture = false),
                            ),
                    ),
            )

        val lexemes = TokenLexemes.from(config)

        assertEquals("let", lexemes.lexemeFor("LET"))
        assertEquals(";", lexemes.lexemeFor("SEMICOLON"))
    }

    @Test
    fun `skips ExactRule tokens with several matchers`() {
        val config =
            LanguageConfig(
                order = listOf("operators"),
                rulesByCategory =
                    mapOf(
                        "operators" to
                            listOf(
                                ExactRule(listOf("+", "-", "*", "/"), "OPERATOR", capture = true),
                            ),
                    ),
            )

        assertNull(TokenLexemes.from(config).lexemeFor("OPERATOR"))
    }

    @Test
    fun `skips RegexRule tokens`() {
        val config =
            LanguageConfig(
                order = listOf("identifiers"),
                rulesByCategory =
                    mapOf(
                        "identifiers" to
                            listOf(
                                RegexRule(
                                    matcher = listOf("^[a-zA-Z_][a-zA-Z0-9_]*$"),
                                    token = "ID",
                                    capture = true,
                                    partial = "^[a-zA-Z_]",
                                ),
                            ),
                    ),
            )

        assertNull(TokenLexemes.from(config).lexemeFor("ID"))
    }

    @Test
    fun `lexemeFor is null when the token type is unknown`() {
        val lexemes = TokenLexemes.from(LanguageConfig(order = emptyList(), rulesByCategory = emptyMap()))

        assertNull(lexemes.lexemeFor("LET"))
    }

    @Test
    fun `later ExactRule with the same token overwrites the lexeme`() {
        val config =
            LanguageConfig(
                order = listOf("a", "b"),
                rulesByCategory =
                    mapOf(
                        "a" to listOf(ExactRule(listOf("let"), "LET", capture = false)),
                        "b" to listOf(ExactRule(listOf("LET"), "LET", capture = false)),
                    ),
            )

        assertEquals("LET", TokenLexemes.from(config).lexemeFor("LET"))
    }
}
