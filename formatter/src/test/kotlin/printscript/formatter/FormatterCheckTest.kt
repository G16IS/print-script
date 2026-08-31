package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.TokenLexemes
import printscript.formatter.support.addition
import printscript.formatter.support.program
import printscript.formatter.support.spaceAroundOperator
import printscript.infrastructure.reader.JSONGrammarConfigReader

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

    private val formatter =
        DefaultFormatterFactory.create(
            listOf(spaceAroundOperator()),
            grammar,
            lexemes,
        )

    @Test
    fun `reports both sides of an operator without spaces`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 3))
        val report = formatter.check(program, "1+2")

        assertEquals(2, report.errors.size)
        assertTrue(report.errors.all { it is WhitespaceMismatch })
    }

    @Test
    fun `accepts spaces around the operator`() {
        val program = program(addition(leftCol = 1, opCol = 3, rightCol = 5))
        val report = formatter.check(program, "1 + 2")

        assertTrue(report.isOk)
        assertEquals(0, report.errors.size)
    }

    @Test
    fun `accumulates only the mismatched side and keeps going`() {
        val program = program(addition(leftCol = 1, opCol = 2, rightCol = 4))
        val report = formatter.check(program, "1+ 2")
        val mismatch = report.errors.single() as WhitespaceMismatch

        assertEquals(1, report.errors.size)
        assertEquals(" ", mismatch.expected)
        assertEquals("", mismatch.actual)
    }
}
