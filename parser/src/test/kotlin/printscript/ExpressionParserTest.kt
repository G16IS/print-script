package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.common.ast.BinaryExpression
import printscript.common.ast.Identifier
import printscript.common.ast.NumberLiteral
import printscript.common.ast.StringLiteral
import printscript.common.domain.Token
import printscript.parser.error.ParseException
import printscript.parser.expression.PrecedenceExpressionParser
import printscript.parser.support.MockLexer
import printscript.parser.support.TokenFactory
import printscript.parser.token.LexerTokenSource
import printscript.parser.token.TokenSource

class ExpressionParserTest {
    private val parser = PrecedenceExpressionParser()

    @BeforeEach
    fun setUp() {
        TokenFactory.reset()
    }

    @Test
    fun `parses number literal`() {
        val expr = parser.parse(source(TokenFactory.number("42")))
        assertTrue(expr is NumberLiteral)
        assertEquals(42.0, (expr as NumberLiteral).value)
    }

    @Test
    fun `parses string literal stripping quotes if present`() {
        val expr = parser.parse(source(TokenFactory.string("\"hola\"")))
        assertTrue(expr is StringLiteral)
        assertEquals("hola", (expr as StringLiteral).value)
    }

    @Test
    fun `parses identifier`() {
        val expr = parser.parse(source(TokenFactory.id("x")))
        assertTrue(expr is Identifier)
        assertEquals("x", (expr as Identifier).name)
    }

    @Test
    fun `respects multiplication over addition precedence`() {
        // 1 + 2 * 3  =>  +(1, *(2, 3))
        val expr = parser.parse(
            source(
                TokenFactory.number("1"),
                TokenFactory.op("+"),
                TokenFactory.number("2"),
                TokenFactory.op("*"),
                TokenFactory.number("3")
            )
        )
        assertTrue(expr is BinaryExpression)
        val add = expr as BinaryExpression
        assertEquals("+", add.operation)
        assertEquals(1.0, (add.left as NumberLiteral).value)
        val mul = add.right as BinaryExpression
        assertEquals("*", mul.operation)
        assertEquals(2.0, (mul.left as NumberLiteral).value)
        assertEquals(3.0, (mul.right as NumberLiteral).value)
    }

    @Test
    fun `parentheses override precedence`() {
        // (1 + 2) * 3  =>  *(+(1, 2), 3)
        val expr = parser.parse(
            source(
                TokenFactory.lparen(),
                TokenFactory.number("1"),
                TokenFactory.op("+"),
                TokenFactory.number("2"),
                TokenFactory.rparen(),
                TokenFactory.op("*"),
                TokenFactory.number("3")
            )
        )
        val mul = expr as BinaryExpression
        assertEquals("*", mul.operation)
        val add = mul.left as BinaryExpression
        assertEquals("+", add.operation)
        assertEquals(1.0, (add.left as NumberLiteral).value)
        assertEquals(2.0, (add.right as NumberLiteral).value)
        assertEquals(3.0, (mul.right as NumberLiteral).value)
    }

    @Test
    fun `left associativity for same precedence`() {
        // 1 - 2 - 3  =>  -(-(1, 2), 3)
        val expr = parser.parse(
            source(
                TokenFactory.number("1"),
                TokenFactory.op("-"),
                TokenFactory.number("2"),
                TokenFactory.op("-"),
                TokenFactory.number("3")
            )
        ) as BinaryExpression
        assertEquals("-", expr.operation)
        val left = expr.left as BinaryExpression
        assertEquals("-", left.operation)
        assertEquals(1.0, (left.left as NumberLiteral).value)
        assertEquals(2.0, (left.right as NumberLiteral).value)
        assertEquals(3.0, (expr.right as NumberLiteral).value)
    }

    @Test
    fun `rejects empty expression`() {
        assertThrows<ParseException> {
            parser.parse(source(TokenFactory.semicolon()))
        }
    }

    private fun source(vararg tokens: Token): TokenSource =
        LexerTokenSource(MockLexer(tokens.toList() + TokenFactory.eof()))
}
