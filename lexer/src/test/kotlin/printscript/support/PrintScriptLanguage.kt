package printscript.support

import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule

/**
 * PrintScript v1 language, same `order` as `language.config.json`.
 *
 * [RuleDrawResolver] picks the **first** matching category in `order`: try
 * keywords, then types, then operators, and so on. Identifiers are last so
 * `let` is `LET` and not `ID`.
 *
 * Number/string `partial`s here accept mid-lexeme prefixes (`1.`, `"hola`) so
 * decimals and unterminated-string errors work. The resource file is narrower.
 */
object PrintScriptLanguage {
    val ORDER = listOf("keywords", "types", "operators", "literals", "identifiers")

    const val NUMBER_PARTIAL = "^[0-9]+(\\.[0-9]*)?$"
    const val STRING_PARTIAL = "^\"[^\"]*$"
    const val PRODUCTION_NUMBER_PARTIAL = "^[0-9]"
    const val PRODUCTION_STRING_PARTIAL = "^\""

    fun config(
        order: List<String> = ORDER,
        numberPartial: String = NUMBER_PARTIAL,
        stringPartial: String = STRING_PARTIAL,
    ): LanguageConfig =
        LanguageConfig(
            order = order,
            config =
                mapOf(
                    "keywords" to keywords(),
                    "types" to types(),
                    "operators" to operators(),
                    "literals" to literals(numberPartial, stringPartial),
                    "identifiers" to identifiers(),
                ),
        )

    fun reversedOrder(): LanguageConfig = config(order = ORDER.reversed())

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

    private fun literals(
        numberPartial: String,
        stringPartial: String,
    ): List<TokenRule> =
        listOf(
            RegexRule(listOf("^\"[^\"]*\""), "STRING_LITERAL", true, stringPartial),
            RegexRule(listOf("^[0-9]+(\\.[0-9]+)?"), "NUMBER_LITERAL", true, numberPartial),
        )

    private fun identifiers(): List<TokenRule> =
        listOf(
            RegexRule(listOf("^[a-zA-Z_][a-zA-Z0-9_]*"), "ID", true, "^[a-zA-Z_]"),
        )
}
