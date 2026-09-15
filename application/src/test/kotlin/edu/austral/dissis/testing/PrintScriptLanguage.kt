package edu.austral.dissis.testing

import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule

/**
 * Same rules and `order` as language.config.v1.0.json. The lexer tries categories
 * from first to last, so keywords beat identifiers.
 */
object PrintScriptLanguage {
    fun config(): LanguageConfig =
        LanguageConfig(
            order = listOf("keywords", "types", "operators", "literals", "identifiers"),
            config =
                mapOf(
                    "keywords" to keywords(),
                    "types" to types(),
                    "operators" to operators(),
                    "literals" to literals(),
                    "identifiers" to identifiers(),
                ),
        )

    private fun keywords(): List<TokenRule> =
        listOf(
            ExactRule(listOf("let"), "LET", false),
            ExactRule(listOf("println"), "CALL", true),
        )

    private fun types(): List<TokenRule> =
        listOf(
            ExactRule(listOf("string", "number"), "TYPE", true),
        )

    private fun operators(): List<TokenRule> =
        listOf(
            ExactRule(listOf(":"), "COLON", false),
            ExactRule(listOf("="), "ASSIGN", false),
            ExactRule(listOf(";"), "SEMICOLON", false),
            ExactRule(listOf("("), "LEFT_PAREN", false),
            ExactRule(listOf(")"), "RIGHT_PAREN", false),
            ExactRule(listOf(","), "COMMA", false),
            ExactRule(listOf("+", "-", "*", "/"), "OPERATOR", true),
        )

    private fun literals(): List<TokenRule> =
        listOf(
            RegexRule(listOf("^\"[^\"]*\""), "STRING_LITERAL", true, "^\"[^\"]*\$"),
            RegexRule(listOf("^'[^']*'"), "STRING_LITERAL", true, "^'[^']*$"),
            RegexRule(listOf("^[0-9]+(\\.[0-9]+)?"), "NUMBER_LITERAL", true, "^[0-9]"),
        )

    private fun identifiers(): List<TokenRule> =
        listOf(
            RegexRule(listOf("^[a-zA-Z_][a-zA-Z0-9_]*"), "ID", true, "^[a-zA-Z_]"),
        )
}
