package printscript

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import printscript.ast.Location
import printscript.domain.Token
import printscript.error.InvalidIdentifierFormat
import printscript.error.InvalidPrintlnArgument
import printscript.reader.CharPosition
import printscript.rule.IdentifierFormatRule
import printscript.rule.LetterCase
import printscript.rule.PrintlnArgumentRule
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

class DefaultLinterTest {
    private fun loc(
        startLine: Int,
        startCol: Int,
        endLine: Int,
        endCol: Int,
    ) = Location(
        start = CharPosition(startLine, startCol),
        end = CharPosition(endLine, endCol),
    )

    private fun variableNode(
        identifier: String,
        location: Location,
    ): SyntaxNode {
        val idNode =
            SyntaxNode(
                name = "ID",
                token = Token("ID", Optional.of(identifier), location),
                location = location,
            )
        return SyntaxNode(
            name = "variable",
            children = listOf(idNode),
            location = location,
        )
    }

    private fun printlnBinaryCall(location: Location): SyntaxNode {
        val callToken =
            SyntaxNode(
                name = "CALL",
                token = Token("CALL", Optional.of("println"), location),
                location = location,
            )
        val leftTerm =
            SyntaxNode(
                name = "term",
                children =
                    listOf(
                        SyntaxNode(
                            name = "number",
                            token = Token("NUMBER", Optional.of("1"), location),
                            location = location,
                        ),
                    ),
                location = location,
            )
        val op =
            SyntaxNode(
                name = "OPERATOR",
                token = Token("OPERATOR", Optional.of("+"), location),
                location = location,
            )
        val rightTerm =
            SyntaxNode(
                name = "term",
                children =
                    listOf(
                        SyntaxNode(
                            name = "number",
                            token = Token("NUMBER", Optional.of("2"), location),
                            location = location,
                        ),
                    ),
                location = location,
            )
        val binaryExpr =
            SyntaxNode(
                name = "expression",
                children = listOf(leftTerm, op, rightTerm),
                location = location,
            )
        return SyntaxNode(
            name = "call",
            children = listOf(callToken, binaryExpr),
            location = location,
        )
    }

    @Test
    fun `empty program yields isOk Report`() {
        val linter = DefaultLinter(listOf(IdentifierFormatRule(LetterCase.CAMEL_CASE)))
        val report = linter.lint(SyntaxProgram.empty())

        assertTrue(report.isOk)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun `clean program yields isOk Report`() {
        val linter =
            DefaultLinter(
                listOf(
                    IdentifierFormatRule(LetterCase.CAMEL_CASE),
                    PrintlnArgumentRule(),
                ),
            )
        val validVar = variableNode("userName", loc(1, 1, 1, 20))
        val program = SyntaxProgram(listOf(validVar), loc(1, 1, 1, 20))

        val report = linter.lint(program)
        assertTrue(report.isOk)
        assertTrue(report.errors.isEmpty())
    }

    @Test
    fun `accumulates multiple violations from different rules in order of location`() {
        val linter =
            DefaultLinter(
                listOf(
                    IdentifierFormatRule(LetterCase.CAMEL_CASE),
                    PrintlnArgumentRule(),
                ),
            )

        // Statement 1 at line 1: bad identifier (snake_case)
        val badVar = variableNode("user_name", loc(1, 5, 1, 14))
        // Statement 2 at line 2: bad println call (binary expression)
        val badCall = printlnBinaryCall(loc(2, 1, 2, 20))

        val program = SyntaxProgram(listOf(badVar, badCall), loc(1, 1, 2, 20))
        val report = linter.lint(program)

        assertFalse(report.isOk)
        assertEquals(2, report.errors.size)
        assertTrue(report.errors[0] is InvalidIdentifierFormat)
        assertTrue(report.errors[1] is InvalidPrintlnArgument)
        assertEquals(
            1,
            report.errors[0]
                .location.start.line,
        )
        assertEquals(
            2,
            report.errors[1]
                .location.start.line,
        )
    }

    @Test
    fun `single statement program returns errors for that statement`() {
        val linter = DefaultLinter(listOf(IdentifierFormatRule(LetterCase.CAMEL_CASE)))
        val badVar = variableNode("bad_name", loc(1, 5, 1, 13))
        val program = SyntaxProgram(listOf(badVar), loc(1, 1, 1, 15))

        val report = linter.lint(program)
        assertFalse(report.isOk)
        assertEquals(1, report.errors.size)
        assertTrue(report.errors.first() is InvalidIdentifierFormat)
    }
}
