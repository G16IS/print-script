package printscript.support

import java.io.File
import java.rmi.UnexpectedException
import printscript.DefaultLexerFactory
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.domain.ExactRule
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.Token
import printscript.domain.TokenRule
import printscript.edition.LanguageCatalog
import printscript.reader.FileCodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.map

/**
 * Parses PrintScript source with the real lexer/parser + grammar.config.v1.0.json.
 *
 * Same rules and `order` as language.config.v1.0.json. The lexer tries categories
 * from first to last, so keywords beat identifiers.
 */
object LinterPsSupport {
    fun parse(code: String): SyntaxProgram {
        val file = File.createTempFile("printscript-linter-test", ".ps").apply { writeText(code) }
        val codeReader = FileCodeReader(file.absolutePath)
        val lexer = DefaultLexerFactory.create(codeReader, language())
        val parser =
            (
                (
                    LanguageCatalog.of("1.0").map { kit ->
                        DefaultParserFactory.create(grammar(), kit.parserHandlers)
                    }
                ) as Result.Ok
            ).value

        var program = SyntaxProgram.empty()
        while (peekNextToken(lexer).type != "EOF") {
            when (
                val parseResult =
                    parser
                        .parseNextStatement(lexer, program)
            ) {
                is Result.Err -> throw UnexpectedException(parseResult.error.message)
                is Result.Ok -> program = parseResult.value
            }
        }
        return program
    }

    private fun peekNextToken(lexer: Lexer): Token {
        when (val token = lexer.peek(null)) {
            is Result.Err -> throw UnexpectedException("Token error")
            is Result.Ok -> return token.value
        }
    }

    private fun language(): LanguageConfig =
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

    private fun grammar(): Grammar {
        val stream =
            requireNotNull(LinterPsSupport::class.java.getResourceAsStream("/grammar.config.v1.0.json")) {
                "Missing resource grammar.config.v1.0.json"
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
