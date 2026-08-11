package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.ast.BinaryExpression
import printscript.ast.CallExpression
import printscript.ast.ExpressionStatement
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.Program
import printscript.ast.StringLiteral
import printscript.ast.VariableStatement
import printscript.ast.VariableType
import printscript.domain.Token
import printscript.error.ParseException
import printscript.support.MockLexer
import printscript.support.TokenFactory

class ParserTest {
    private lateinit var parser: Parser

    @BeforeEach
    fun setUp() {
        TokenFactory.reset()
        parser = DefaultParserFactory.create()
    }

    @Test
    fun `empty program is returned when no statements are parsed`() {
        val program = Program.empty()
        assertTrue(program.statements.isEmpty())
    }

    @Test
    fun `rejects stream that only has EOF`() {
        val lexer = MockLexer(TokenFactory.program())
        assertThrows<ParseException> {
            parser.parseNextStatement(lexer, Program.empty())
        }
    }

    @Test
    fun `parses simple variable declaration`() {
        // let x: number = 5;
        val lexer = mockLexer(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("5"),
            TokenFactory.semicolon()
        )
        val program = parser.parseNextStatement(lexer, Program.empty())
        assertEquals(1, program.statements.size)
        val stmt = program.statements[0] as VariableStatement
        assertEquals("x", stmt.declaration.id.name)
        assertEquals(VariableType.NUMBER, stmt.declaration.typeAnnotation)
        assertEquals(5.0, (stmt.declaration.initializer as NumberLiteral).value)
    }

    @Test
    fun `parses variable declaration with binary expression`() {
        // let x: number = 1 + 2;
        val lexer = mockLexer(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("1"),
            TokenFactory.op("+"),
            TokenFactory.number("2"),
            TokenFactory.semicolon()
        )
        val program = parser.parseNextStatement(lexer, Program.empty())
        val stmt = program.statements[0] as VariableStatement
        val init = stmt.declaration.initializer as BinaryExpression
        assertEquals("+", init.operation)
        assertEquals(1.0, (init.left as NumberLiteral).value)
        assertEquals(2.0, (init.right as NumberLiteral).value)
    }

    @Test
    fun `parses println with identifier`() {
        // println(x);
        val lexer = mockLexer(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.id("x"),
            TokenFactory.rparen(),
            TokenFactory.semicolon()
        )
        val program = parser.parseNextStatement(lexer, Program.empty())
        val stmt = program.statements[0] as ExpressionStatement
        val call = stmt.expression as CallExpression
        assertEquals("println", call.callee)
        assertEquals(1, call.args.size)
        assertEquals("x", (call.args[0] as Identifier).name)
    }

    @Test
    fun `parses println with expression`() {
        // println(1 + 2);
        val lexer = mockLexer(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.number("1"),
            TokenFactory.op("+"),
            TokenFactory.number("2"),
            TokenFactory.rparen(),
            TokenFactory.semicolon()
        )
        val program = parser.parseNextStatement(lexer, Program.empty())
        val call = (program.statements[0] as ExpressionStatement).expression as CallExpression
        val arg = call.args[0] as BinaryExpression
        assertEquals("+", arg.operation)
    }

    @Test
    fun `parses program with let and println via successive parseNextStatement`() {
        // let x: number = 10; println(x);
        val lexer = mockLexer(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("10"),
            TokenFactory.semicolon(),
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.id("x"),
            TokenFactory.rparen(),
            TokenFactory.semicolon()
        )
        var program = Program.empty()
        program = parser.parseNextStatement(lexer, program)
        program = parser.parseNextStatement(lexer, program)
        assertEquals(2, program.statements.size)
        assertTrue(program.statements[0] is VariableStatement)
        assertTrue(program.statements[1] is ExpressionStatement)
    }

    @Test
    fun `does not reject semantic type mismatch`() {
        // let x: number = "hola";  — syntactically valid
        val lexer = mockLexer(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.string("hola"),
            TokenFactory.semicolon()
        )
        val program = parser.parseNextStatement(lexer, Program.empty())
        val stmt = program.statements[0] as VariableStatement
        assertEquals(VariableType.NUMBER, stmt.declaration.typeAnnotation)
        assertTrue(stmt.declaration.initializer is StringLiteral)
    }

    @Test
    fun `rejects missing colon in declaration`() {
        val lexer = mockLexer(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("1"),
            TokenFactory.semicolon()
        )
        assertThrows<ParseException> {
            parser.parseNextStatement(lexer, Program.empty())
        }
    }

    @Test
    fun `rejects missing semicolon after println`() {
        val lexer = mockLexer(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.id("x"),
            TokenFactory.rparen()
        )
        assertThrows<ParseException> {
            parser.parseNextStatement(lexer, Program.empty())
        }
    }

    @Test
    fun `rejects unexpected token at statement start`() {
        val lexer = mockLexer(
            TokenFactory.number("1"),
            TokenFactory.semicolon()
        )
        assertThrows<ParseException> {
            parser.parseNextStatement(lexer, Program.empty())
        }
    }

    private fun mockLexer(vararg tokens: Token): MockLexer =
        MockLexer(TokenFactory.program(*tokens))
}
