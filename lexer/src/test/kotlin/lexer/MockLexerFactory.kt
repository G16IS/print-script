package lexer

import printscript.Lexer
import printscript.RuleDrawResolver
import printscript.TokenStream
import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule
import printscript.evaluator.RuleEvaluator

class MockLexerFactory {
    companion object{
        fun create(statement: String): Lexer {
            val tokenReader = MockReader(statement)
            val config = createMockConfig()
            val ruleEvaluator = RuleEvaluator(config)
            val ruleDrawResolver = RuleDrawResolver(LanguageConfig(createOrder(), config))
            return TokenStream(tokenReader, ruleEvaluator, ruleDrawResolver)
        }

        private fun createOrder(): List<String> =
            listOf("keywords", "types", "operators", "literals", "identifiers")

        private fun createMockConfig(): Map<String, List<TokenRule>> =
            mapOf(
                "keywords" to listOf(
                    ExactRule(listOf("let"), "LET", false),
                    ExactRule(listOf("println"), "CALL", true)
                ),
                "types" to listOf(
                    ExactRule(listOf("string", "number"), "TYPE", true)
                ),
                "operators" to listOf(
                    ExactRule(listOf(":"), "COLON", false),
                    ExactRule(listOf("="), "ASSIGN", false),
                    ExactRule(listOf(";"), "SEMICOLON", false),
                    ExactRule(listOf("("), "LEFT_PAREN", false),
                    ExactRule(listOf(")"), "RIGHT_PAREN", false),
                    ExactRule(listOf(","), "COMMA", false)
                ),
                "literals" to listOf(
                    RegexRule(listOf("^\"[^\"]*\""), "STRING_LITERAL", true, "^\""),
                    RegexRule(listOf("^[0-9]+(\\.[0-9]+)?"), "NUMBER_LITERAL", true, "^[0-9]")
                ),
                "identifiers" to listOf(
                    RegexRule(listOf("^[a-zA-Z_][a-zA-Z0-9_]*"), "ID", true, "^[a-zA-Z_]")
                )
            )
    }

}
