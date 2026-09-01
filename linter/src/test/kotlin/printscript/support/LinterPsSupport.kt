package printscript.support

import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.domain.ExactRule
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.StringCodeReader
import printscript.syntax.SyntaxProgram

object LinterPsSupport {
    fun parse(code: String): SyntaxProgram {
        val codeReader = StringCodeReader(code)
        val lexer = DefaultLexerFactory.create(codeReader, language())
        val parser = DefaultParserFactory.create(grammar())

        var program = SyntaxProgram.empty()
        while (lexer.peek(null).type != "EOF") {
            program = parser.parseNextStatement(lexer, program)
        }
        return program
    }

    private fun language(): LanguageConfig =
        LanguageConfig(
            order = listOf("identifiers", "literals", "operators", "types", "keywords"),
            config =
                mapOf(
                    "keywords" to keywords(),
                    "types" to types(),
                    "operators" to operators(),
                    "literals" to literals(),
                    "identifiers" to identifiers(),
                ),
        )

    private fun grammar(): Grammar {
        val stream =
            requireNotNull(LinterPsSupport::class.java.getResourceAsStream("/grammar.config.json")) {
                "Missing resource grammar.config.json"
            }
        return JSONGrammarConfigReader.read(stream)
    }

    private fun keywords(): List<TokenRule> =
        listOf(
            ExactRule(listOf("let"), "LET", false),
            ExactRule(listOf("println"), "CALL", true),
        )

    private fun types(): List<TokenRule> = listOf(ExactRule(listOf("string", "number"), "TYPE", true))

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
            RegexRule(listOf("^\"[^\"]*\""), "STRING_LITERAL", true, "^\"[^\"]*$"),
            RegexRule(listOf("^[0-9]+(\\.[0-9]+)?"), "NUMBER_LITERAL", true, "^[0-9]+(\\.[0-9]*)?$"),
        )

    private fun identifiers(): List<TokenRule> =
        listOf(RegexRule(listOf("^[a-zA-Z_][a-zA-Z0-9_]*"), "ID", true, "^[a-zA-Z_]"))
}
