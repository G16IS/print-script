package printscript.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.common.ast.BinaryExpression
import printscript.common.ast.CallExpression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.Identifier
import printscript.common.ast.NumberLiteral
import printscript.common.ast.StringLiteral
import printscript.common.ast.VariableStatement
import printscript.parser.error.ParseException
import printscript.parser.support.TokenFactory

class ParserTest {
    @BeforeEach
    fun setUp() {
        TokenFactory.reset()
    }

    @Test
    fun `empty token list yields empty program`() {
        val program = parse(emptyList())
        assertTrue(program.statements.isEmpty())
    }

    @Test
    fun `parses simple variable declaration`() {
        // let x: number = 5;
        val tokens = TokenFactory.program(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("5"),
            TokenFactory.semicolon()
        )
        val program = parse(tokens)
        assertEquals(1, program.statements.size)
        val stmt = program.statements[0] as VariableStatement
        assertEquals("x", stmt.declaration.id.name)
        assertEquals("number", stmt.declaration.typeAnnotation)
        assertEquals(5.0, (stmt.declaration.initializer as NumberLiteral).value)
    }

    @Test
    fun `parses variable declaration with binary expression`() {
        // let x: number = 1 + 2;
        val tokens = TokenFactory.program(
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
        val program = parse(tokens)
        val stmt = program.statements[0] as VariableStatement
        val init = stmt.declaration.initializer as BinaryExpression
        assertEquals("+", init.operation)
        assertEquals(1.0, (init.left as NumberLiteral).value)
        assertEquals(2.0, (init.right as NumberLiteral).value)
    }

    @Test
    fun `parses println with identifier`() {
        // println(x);
        val tokens = TokenFactory.program(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.id("x"),
            TokenFactory.rparen(),
            TokenFactory.semicolon()
        )
        val program = parse(tokens)
        val stmt = program.statements[0] as ExpressionStatement
        val call = stmt.expression as CallExpression
        assertEquals("println", call.callee)
        assertEquals(1, call.args.size)
        assertEquals("x", (call.args[0] as Identifier).name)
    }

    @Test
    fun `parses println with expression`() {
        // println(1 + 2);
        val tokens = TokenFactory.program(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.number("1"),
            TokenFactory.op("+"),
            TokenFactory.number("2"),
            TokenFactory.rparen(),
            TokenFactory.semicolon()
        )
        val program = parse(tokens)
        val call = (program.statements[0] as ExpressionStatement).expression as CallExpression
        val arg = call.args[0] as BinaryExpression
        assertEquals("+", arg.operation)
    }

    @Test
    fun `parses program with let and println`() {
        // let x: number = 10; println(x);
        val tokens = TokenFactory.program(
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
        val program = parse(tokens)
        assertEquals(2, program.statements.size)
        assertTrue(program.statements[0] is VariableStatement)
        assertTrue(program.statements[1] is ExpressionStatement)
    }

    @Test
    fun `does not reject semantic type mismatch`() {
        // let x: number = "hola";  — syntactically valid
        val tokens = TokenFactory.program(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.colon(),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.string("hola"),
            TokenFactory.semicolon()
        )
        val program = parse(tokens)
        val stmt = program.statements[0] as VariableStatement
        assertEquals("number", stmt.declaration.typeAnnotation)
        assertTrue(stmt.declaration.initializer is StringLiteral)
    }

    @Test
    fun `rejects missing colon in declaration`() {
        val tokens = TokenFactory.program(
            TokenFactory.let(),
            TokenFactory.id("x"),
            TokenFactory.type("number"),
            TokenFactory.assign(),
            TokenFactory.number("1"),
            TokenFactory.semicolon()
        )
        assertThrows<ParseException> { parse(tokens) }
    }

    @Test
    fun `rejects missing semicolon after println`() {
        val tokens = TokenFactory.program(
            TokenFactory.print(),
            TokenFactory.lparen(),
            TokenFactory.id("x"),
            TokenFactory.rparen()
        )
        assertThrows<ParseException> { parse(tokens) }
    }

    @Test
    fun `rejects unexpected token at statement start`() {
        val tokens = TokenFactory.program(
            TokenFactory.number("1"),
            TokenFactory.semicolon()
        )
        assertThrows<ParseException> { parse(tokens) }
    }
}
