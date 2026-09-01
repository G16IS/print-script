package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import printscript.ast.Location
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig
import printscript.domain.TokenLexemes
import printscript.error.FormatError
import printscript.formatter.support.addition
import printscript.formatter.support.leaf
import printscript.formatter.support.number
import printscript.formatter.support.program
import printscript.formatter.support.wrap
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.syntax.SyntaxNode
import printscript.util.Result

class PrintScriptLayoutTest {
    private val grammar =
        JSONGrammarConfigReader.read(
            checkNotNull(javaClass.getResourceAsStream("/grammar.config.json")) {
                "Missing grammar.config.json"
            },
        )
    private val lexemes =
        TokenLexemes(
            mapOf(
                "LET" to "let",
                "COLON" to ":",
                "ASSIGN" to "=",
                "SEMICOLON" to ";",
                "LEFT_PAREN" to "(",
                "RIGHT_PAREN" to ")",
            ),
        )

    private val formatter = formatterFromLanguage()

    @Test
    fun `formats a declaration with default spaces and newline after semicolon`() {
        val statement =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        leaf("ID", "x", 1),
                        leaf("TYPE", "number", 1),
                        wrap("expression", wrap("term", number("1", 1))),
                    ),
                location = Location.empty(),
            )

        assertEquals("let x : number = 1;\n", ok(formatter.format(program(statement))))
    }

    @Test
    fun `disabled colon spaces omit the gap`() {
        val user =
            FormatterRulesConfig(
                listOf(
                    FormatRuleSpec(type = "space-before-colon", enabled = false),
                    FormatRuleSpec(type = "space-after-colon", enabled = false),
                    FormatRuleSpec(type = "space-around-assign", enabled = true),
                    FormatRuleSpec(type = "newlines-before-println", count = 1),
                ),
            )
        val formatter = formatterFromLanguage(user)
        val statement =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        leaf("ID", "x", 1),
                        leaf("TYPE", "number", 1),
                        wrap("expression", wrap("term", number("1", 1))),
                    ),
                location = Location.empty(),
            )

        assertEquals("let x:number = 1;\n", ok(formatter.format(program(statement))))
    }

    @Test
    fun `println after a statement gets extra newlines`() {
        val first =
            SyntaxNode(
                name = "expression-stmt",
                children = listOf(addition(1, 2, 3)),
                location = Location.empty(),
            )
        val print =
            SyntaxNode(
                name = "expression-stmt",
                children =
                    listOf(
                        wrap(
                            "expression",
                            wrap(
                                "term",
                                SyntaxNode(
                                    name = "call",
                                    children =
                                        listOf(
                                            leaf("CALL", "println", 1),
                                            wrap("expression", wrap("term", number("1", 1))),
                                        ),
                                    location = Location.empty(),
                                ),
                            ),
                        ),
                    ),
                location = Location.empty(),
            )

        assertEquals("1 + 2;\n\nprintln(1);\n", ok(formatter.format(program(first, print))))
    }

    private fun formatterFromLanguage(user: FormatterRulesConfig = FormatterRulesConfig()): Formatter {
        val language = JSONFormatterLanguageConfigReader.read(languageResource())
        val defaults = JSONFormatterRulesConfigReader.read(defaultsResource())
        val result = DefaultFormatterFactory.createFromConfig(language, user, defaults, grammar, lexemes)

        return (result as Result.Ok).value
    }

    private fun languageResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-language.json")) {
            "Missing formatter-language.json"
        }

    private fun defaultsResource() =
        checkNotNull(javaClass.getResourceAsStream("/formatter-user-defaults.json")) {
            "Missing formatter-user-defaults.json"
        }

    private fun ok(result: Result<String, FormatError>): String {
        assertEquals(true, result is Result.Ok, "expected Ok but was $result")

        return (result as Result.Ok).value
    }
}
