package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.domain.Token
import printscript.error.ParseException
import printscript.support.MockLexer
import printscript.support.PrintScriptGrammar
import printscript.support.Tokens
import printscript.support.lhs
import printscript.support.op
import printscript.support.rhs
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

class ParserTest {
    private lateinit var parser: Parser

    @BeforeEach
    fun setUp() {
        Tokens.reset()
        parser = DefaultParserFactory.create(PrintScriptGrammar)
    }

    @Test
    fun `empty program is returned when no statements are parsed`() {
        assertTrue(SyntaxProgram.empty().statements.isEmpty())
    }

    @Test
    fun `rejects stream that only has EOF`() {
        assertThrows<ParseException> {
            parser.parseNextStatement(MockLexer(listOf(Tokens.eof())), SyntaxProgram.empty())
        }
    }

    @Test
    fun `parses simple variable declaration`() {
        val stmt = parseOne(
            Tokens.let(),
            Tokens.id("x"),
            Tokens.colon(),
            Tokens.type("number"),
            Tokens.assign(),
            Tokens.number("5"),
            Tokens.semicolon()
        )
        assertEquals("variable", stmt.name)
        assertEquals("x", stmt.child("ID").value())
        assertEquals("number", stmt.child("TYPE").value())
        assertEquals("5", numberValue(stmt.child("expression")))
    }

    @Test
    fun `parses variable declaration with binary expression`() {
        val stmt = parseOne(
            Tokens.let(),
            Tokens.id("x"),
            Tokens.colon(),
            Tokens.type("number"),
            Tokens.assign(),
            Tokens.number("1"),
            Tokens.op("+"),
            Tokens.number("2"),
            Tokens.semicolon()
        )
        val expr = stmt.child("expression")
        assertEquals("+", expr.op())
        assertEquals("1", numberValue(expr.lhs()))
        assertEquals("2", numberValue(expr.rhs()))
    }

    @Test
    fun `parses println with identifier`() {
        val stmt = parseOne(
            Tokens.print(),
            Tokens.lparen(),
            Tokens.id("x"),
            Tokens.rparen(),
            Tokens.semicolon()
        )
        val call = callOf(stmt)
        assertEquals("println", call.child("CALL").value())
        assertEquals("x", identifierValue(call.child("expression")))
    }

    @Test
    fun `parses println with expression`() {
        val stmt = parseOne(
            Tokens.print(),
            Tokens.lparen(),
            Tokens.number("1"),
            Tokens.op("+"),
            Tokens.number("2"),
            Tokens.rparen(),
            Tokens.semicolon()
        )
        val arg = callOf(stmt).child("expression")
        assertEquals("+", arg.op())
    }

    @Test
    fun `parses successive let and println statements`() {
        val lexer = mockLexer(
            Tokens.let(),
            Tokens.id("x"),
            Tokens.colon(),
            Tokens.type("number"),
            Tokens.assign(),
            Tokens.number("10"),
            Tokens.semicolon(),
            Tokens.print(),
            Tokens.lparen(),
            Tokens.id("x"),
            Tokens.rparen(),
            Tokens.semicolon()
        )
        var program = SyntaxProgram.empty()
        program = parser.parseNextStatement(lexer, program)
        program = parser.parseNextStatement(lexer, program)
        assertEquals(listOf("variable", "expression-stmt"), program.statements.map { it.name })
    }

    @Test
    fun `does not reject semantic type mismatch`() {
        val stmt = parseOne(
            Tokens.let(),
            Tokens.id("x"),
            Tokens.colon(),
            Tokens.type("number"),
            Tokens.assign(),
            Tokens.string("hola"),
            Tokens.semicolon()
        )
        assertEquals("number", stmt.child("TYPE").value())
        assertEquals("string", stmt.child("expression").find("string").name)
    }

    @Test
    fun `parses a bare expression statement`() {
        val stmt = parseOne(
            Tokens.number("1"),
            Tokens.op("+"),
            Tokens.number("2"),
            Tokens.semicolon()
        )
        assertEquals("expression-stmt", stmt.name)
        assertEquals("+", stmt.child("expression").op())
    }

    @Test
    fun `rejects missing colon in declaration`() {
        assertThrows<ParseException> {
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.type("number"),
                Tokens.assign(),
                Tokens.number("1"),
                Tokens.semicolon()
            )
        }
    }

    @Test
    fun `rejects missing semicolon after println`() {
        assertThrows<ParseException> {
            parseOne(
                Tokens.print(),
                Tokens.lparen(),
                Tokens.id("x"),
                Tokens.rparen()
            )
        }
    }

    @Test
    fun `rejects unexpected token at the start of a statement`() {
        assertThrows<ParseException> {
            parseOne(Tokens.colon())
        }
    }

    @Test
    fun `respects multiplication over addition precedence`() {
        val expr = parseOne(
            Tokens.number("1"),
            Tokens.op("+"),
            Tokens.number("2"),
            Tokens.op("*"),
            Tokens.number("3"),
            Tokens.semicolon()
        ).child("expression")
        assertEquals("+", expr.op())
        assertEquals("1", numberValue(expr.lhs()))
        assertEquals("*", expr.rhs().op())
        assertEquals("2", numberValue(expr.rhs().lhs()))
        assertEquals("3", numberValue(expr.rhs().rhs()))
    }

    @Test
    fun `parentheses override precedence`() {
        val expr = parseOne(
            Tokens.lparen(),
            Tokens.number("1"),
            Tokens.op("+"),
            Tokens.number("2"),
            Tokens.rparen(),
            Tokens.op("*"),
            Tokens.number("3"),
            Tokens.semicolon()
        ).child("expression")
        val mul = expr.children.single()
        assertEquals("*", mul.op())
        assertEquals("group", mul.lhs().name)
        assertEquals("+", mul.lhs().child("expression").op())
        assertEquals("3", numberValue(mul.rhs()))
    }

    @Test
    fun `left associativity for same precedence`() {
        val expr = parseOne(
            Tokens.number("1"),
            Tokens.op("-"),
            Tokens.number("2"),
            Tokens.op("-"),
            Tokens.number("3"),
            Tokens.semicolon()
        ).child("expression")
        assertEquals("-", expr.op())
        assertEquals("-", expr.lhs().op())
        assertEquals("1", numberValue(expr.lhs().lhs()))
        assertEquals("2", numberValue(expr.lhs().rhs()))
        assertEquals("3", numberValue(expr.rhs()))
    }

    private fun parseOne(vararg tokens: Token) =
        parser.parseNextStatement(mockLexer(*tokens), SyntaxProgram.empty()).statements.single()

    private fun mockLexer(vararg tokens: Token) = MockLexer(tokens.toList())

    private fun callOf(stmt: SyntaxNode): SyntaxNode =
        stmt.child("expression").find("call")

    private fun numberValue(node: SyntaxNode): String =
        node.find("number").value()

    private fun identifierValue(node: SyntaxNode): String =
        node.find("identifier").value()
}
