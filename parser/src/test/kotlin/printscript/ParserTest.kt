package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import printscript.domain.Token
import printscript.edition.LanguageCatalog
import printscript.error.ParserError
import printscript.io.DefaultSideEffectManager
import printscript.support.MockLexer
import printscript.support.PrintScriptGrammar
import printscript.support.Tokens
import printscript.support.lhs
import printscript.support.op
import printscript.support.rhs
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.map

class ParserTest {
    private lateinit var parser: Parser

    @BeforeEach
    fun setUp() {
        Tokens.reset()
        parser =
            (
                (
                    LanguageCatalog.of("1.0", DefaultSideEffectManager()).map { kit ->
                        DefaultParserFactory.create(PrintScriptGrammar, kit.parserHandlers)
                    }
                ) as Result.Ok
            ).value
    }

    @Test
    fun `empty program is returned when no statements are parsed`() {
        assertTrue(SyntaxProgram.empty().statements.isEmpty())
    }

    @Test
    fun `rejects stream that only has EOF`() {
        val result = parseNext(MockLexer(listOf(Tokens.eof())))
        assertTrue(result is Result.Err)
    }

    @Test
    fun `parses simple variable declaration`() {
        val stmt =
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.colon(),
                Tokens.type("number"),
                Tokens.assign(),
                Tokens.number("5"),
                Tokens.semicolon(),
            )
        assertEquals("variable", stmt.name)
        assertEquals("x", stmt.child("ID").value())
        assertEquals("number", stmt.child("TYPE").value())
        assertEquals("5", numberValue(stmt.find("expression")))
    }

    @Test
    fun `parses variable declaration without initializer`() {
        val stmt =
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.colon(),
                Tokens.type("string"),
                Tokens.semicolon(),
            )
        assertEquals("variable", stmt.name)
        assertEquals("x", stmt.child("ID").value())
        assertEquals("string", stmt.child("TYPE").value())
        val initializer = stmt.child("initializer")
        assertEquals(0, initializer.children.size)
    }

    @Test
    fun `parses variable declaration with initializer nested under optional`() {
        val stmt =
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.colon(),
                Tokens.type("number"),
                Tokens.assign(),
                Tokens.number("5"),
                Tokens.semicolon(),
            )
        val initializer = stmt.child("initializer")
        assertEquals(1, initializer.children.size)
        assertEquals("var-init", initializer.children.single().name)
        assertEquals("5", numberValue(stmt.find("expression")))
    }

    @Test
    fun `parses variable declaration with binary expression`() {
        val stmt =
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.colon(),
                Tokens.type("number"),
                Tokens.assign(),
                Tokens.number("1"),
                Tokens.op("+"),
                Tokens.number("2"),
                Tokens.semicolon(),
            )
        val expr = stmt.find("expression")
        assertEquals("+", expr.op())
        assertEquals("1", numberValue(expr.lhs()))
        assertEquals("2", numberValue(expr.rhs()))
    }

    @Test
    fun `parses println with identifier`() {
        val stmt =
            parseOne(
                Tokens.print(),
                Tokens.lparen(),
                Tokens.id("x"),
                Tokens.rparen(),
                Tokens.semicolon(),
            )
        val call = callOf(stmt)
        assertEquals("println", call.child("CALL").value())
        assertEquals("x", identifierValue(call.child("expression")))
    }

    @Test
    fun `parses println with expression`() {
        val stmt =
            parseOne(
                Tokens.print(),
                Tokens.lparen(),
                Tokens.number("1"),
                Tokens.op("+"),
                Tokens.number("2"),
                Tokens.rparen(),
                Tokens.semicolon(),
            )
        val arg = callOf(stmt).child("expression")
        assertEquals("+", arg.op())
    }

    @Test
    fun `parses successive let and println statements`() {
        val lexer =
            mockLexer(
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
                Tokens.semicolon(),
            )
        var program = SyntaxProgram.empty()
        program = parseOk(parser.parseNextStatement(lexer, program))
        program = parseOk(parser.parseNextStatement(lexer, program))
        assertEquals(listOf("variable", "expression-stmt"), program.statements.map { it.name })
    }

    @Test
    fun `does not reject semantic type mismatch`() {
        val stmt =
            parseOne(
                Tokens.let(),
                Tokens.id("x"),
                Tokens.colon(),
                Tokens.type("number"),
                Tokens.assign(),
                Tokens.string("hola"),
                Tokens.semicolon(),
            )
        assertEquals("number", stmt.child("TYPE").value())
        assertEquals("string", stmt.find("expression").find("string").name)
    }

    @Test
    fun `parses a bare expression statement`() {
        val stmt =
            parseOne(
                Tokens.number("1"),
                Tokens.op("+"),
                Tokens.number("2"),
                Tokens.semicolon(),
            )
        assertEquals("expression-stmt", stmt.name)
        assertEquals("+", stmt.child("expression").op())
    }

    @Test
    fun `parses a standalone assignment`() {
        val stmt =
            parseOne(
                Tokens.id("x"),
                Tokens.assign(),
                Tokens.number("5"),
                Tokens.semicolon(),
            )
        assertEquals("assignment", stmt.name)
        assertEquals("x", stmt.child("ID").value())
        assertEquals("5", numberValue(stmt.find("expression")))
    }

    @Test
    fun `an expression statement starting with an identifier is not an assignment`() {
        // `assignment` va antes que `expression-stmt` en el `or`, así que esto ejercita el
        // backtracking del `try`: matchea ID, falla en ASSIGN, rebobina y cae a expression-stmt.
        val stmt =
            parseOne(
                Tokens.id("x"),
                Tokens.op("+"),
                Tokens.number("1"),
                Tokens.semicolon(),
            )
        assertEquals("expression-stmt", stmt.name)
        assertEquals("+", stmt.child("expression").op())
    }

    @Test
    fun `rejects missing colon in declaration`() {
        val result =
            parseNext(
                mockLexer(
                    Tokens.let(),
                    Tokens.id("x"),
                    Tokens.type("number"),
                    Tokens.assign(),
                    Tokens.number("1"),
                    Tokens.semicolon(),
                ),
            )
        assertTrue(result is Result.Err)
    }

    @Test
    fun `rejects missing semicolon after println`() {
        val result =
            parseNext(
                mockLexer(
                    Tokens.print(),
                    Tokens.lparen(),
                    Tokens.id("x"),
                    Tokens.rparen(),
                ),
            )
        assertTrue(result is Result.Err)
    }

    @Test
    fun `rejects unexpected token at the start of a statement`() {
        val result = parseNext(mockLexer(Tokens.colon()))
        assertTrue(result is Result.Err)
    }

    @Test
    fun `respects multiplication over addition precedence`() {
        val expr =
            parseOne(
                Tokens.number("1"),
                Tokens.op("+"),
                Tokens.number("2"),
                Tokens.op("*"),
                Tokens.number("3"),
                Tokens.semicolon(),
            ).child("expression")
        assertEquals("+", expr.op())
        assertEquals("1", numberValue(expr.lhs()))
        assertEquals("*", expr.rhs().op())
        assertEquals("2", numberValue(expr.rhs().lhs()))
        assertEquals("3", numberValue(expr.rhs().rhs()))
    }

    @Test
    fun `parentheses override precedence`() {
        val expr =
            parseOne(
                Tokens.lparen(),
                Tokens.number("1"),
                Tokens.op("+"),
                Tokens.number("2"),
                Tokens.rparen(),
                Tokens.op("*"),
                Tokens.number("3"),
                Tokens.semicolon(),
            ).child("expression")
        val mul = expr.children.single()
        assertEquals("*", mul.op())
        assertEquals("group", mul.lhs().name)
        assertEquals("+", mul.lhs().child("expression").op())
        assertEquals("3", numberValue(mul.rhs()))
    }

    @Test
    fun `left associativity for same precedence`() {
        val expr =
            parseOne(
                Tokens.number("1"),
                Tokens.op("-"),
                Tokens.number("2"),
                Tokens.op("-"),
                Tokens.number("3"),
                Tokens.semicolon(),
            ).child("expression")
        assertEquals("-", expr.op())
        assertEquals("-", expr.lhs().op())
        assertEquals("1", numberValue(expr.lhs().lhs()))
        assertEquals("2", numberValue(expr.lhs().rhs()))
        assertEquals("3", numberValue(expr.rhs()))
    }

    private fun parseOne(vararg tokens: Token): SyntaxNode =
        parseOk(parser.parseNextStatement(mockLexer(*tokens), SyntaxProgram.empty()))
            .statements
            .single()

    private fun parseNext(lexer: MockLexer): Result<SyntaxProgram, ParserError> =
        parser.parseNextStatement(lexer, SyntaxProgram.empty())

    private fun parseOk(result: Result<SyntaxProgram, ParserError>): SyntaxProgram =
        when (result) {
            is Result.Ok -> result.value
            is Result.Err -> error("Expected a parsed program, got: ${result.error.message}")
        }

    private fun mockLexer(vararg tokens: Token) = MockLexer(tokens.toList())

    private fun callOf(stmt: SyntaxNode): SyntaxNode = stmt.child("expression").find("call")

    private fun numberValue(node: SyntaxNode): String = node.find("number").value()

    private fun identifierValue(node: SyntaxNode): String = node.find("identifier").value()
}
