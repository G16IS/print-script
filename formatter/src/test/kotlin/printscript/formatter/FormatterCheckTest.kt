package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.ast.Location
import printscript.domain.TokenLexemes
import printscript.error.WhitespaceMismatch
import printscript.formatter.support.addition
import printscript.formatter.support.leaf
import printscript.formatter.support.number
import printscript.formatter.support.program
import printscript.formatter.support.spaceAroundOperator
import printscript.formatter.support.wrap
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.syntax.SyntaxNode
import printscript.util.Result

class FormatterCheckTest {
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

    private val operators =
        DefaultFormatterFactory.create(
            listOf(spaceAroundOperator()),
            grammar,
            lexemes,
        )

    private val printScript = formatterFromLanguage()

    @Test
    fun `reports both sides of an operator without spaces`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 3))
        val report = operators.check(program, "1+2")

        assertEquals(2, report.errors.size)
        assertTrue(report.errors.all { it is WhitespaceMismatch })
    }

    @Test
    fun `accepts spaces around the operator`() {
        val program = program(addition(leftCol = 1, opCol = 3, rightCol = 5))
        val report = operators.check(program, "1 + 2")

        assertTrue(report.isOk)
        assertEquals(0, report.errors.size)
    }

    @Test
    fun `accumulates only the mismatched side and keeps going`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 4))
        val report = operators.check(program, "1+ 2")
        val mismatch = report.errors.single() as WhitespaceMismatch

        assertEquals(1, report.errors.size)
        assertEquals(" ", mismatch.expected)
        assertEquals("", mismatch.actual)
    }

    @Test
    fun `reports extra spaces when expected is empty`() {
        val program = program(addition(leftCol = 2, opCol = 4, rightCol = 6))
        val report = operators.check(program, " 1 + 2")
        val mismatch = report.errors.single() as WhitespaceMismatch

        assertEquals("", mismatch.expected)
        assertEquals(" ", mismatch.actual)
    }

    @Test
    fun `checks synthetic semicolon`() {
        val statement =
            SyntaxNode(
                name = "expression-stmt",
                children = listOf(addition(leftCol = 1, opCol = 3, rightCol = 5)),
                location = Location.empty(),
            )
        val report = operators.check(program(statement), "1 + 2;")

        assertTrue(report.isOk)
    }

    @Test
    fun `reports missing trailing newline after semicolon`() {
        val statement =
            SyntaxNode(
                name = "expression-stmt",
                children = listOf(addition(leftCol = 1, opCol = 3, rightCol = 5)),
                location = Location.empty(),
            )
        val report = printScript.check(program(statement), "1 + 2;")
        val mismatch = report.errors.single() as WhitespaceMismatch

        assertEquals("\n", mismatch.expected)
        assertEquals("", mismatch.actual)
    }

    @Test
    fun `reports colon and assign mismatches on a declaration`() {
        val report = printScript.check(program(compactDeclaration()), "let x:number=1;\n")

        assertTrue(report.errors.size >= 4)
        assertTrue(report.errors.all { it is WhitespaceMismatch })
    }

    @Test
    fun `accepts a formatted declaration`() {
        val report = printScript.check(program(spacedDeclaration()), "let x : number = 1;\n")

        assertTrue(report.isOk)
    }

    private fun compactDeclaration(): SyntaxNode =
        SyntaxNode(
            name = "variable",
            children =
                listOf(
                    leaf("ID", "x", 5),
                    leaf("TYPE", "number", 7),
                    wrap("expression", wrap("term", number("1", 14))),
                ),
            location = Location.empty(),
        )

    private fun spacedDeclaration(): SyntaxNode =
        SyntaxNode(
            name = "variable",
            children =
                listOf(
                    leaf("ID", "x", 5),
                    leaf("TYPE", "number", 9),
                    wrap("expression", wrap("term", number("1", 18))),
                ),
            location = Location.empty(),
        )

    private fun formatterFromLanguage(): Formatter {
        val language =
            JSONFormatterLanguageConfigReader.read(
                checkNotNull(javaClass.getResourceAsStream("/formatter-language.json")),
            )
        val defaults =
            JSONFormatterRulesConfigReader.read(
                checkNotNull(javaClass.getResourceAsStream("/formatter-user-defaults.json")),
            )
        val result =
            DefaultFormatterFactory.createFromConfig(
                language,
                defaults = defaults,
                grammar = grammar,
                lexemes = lexemes,
            )

        return (result as Result.Ok).value
    }
}
