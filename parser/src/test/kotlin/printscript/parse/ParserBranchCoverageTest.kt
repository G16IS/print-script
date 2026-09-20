package printscript.parse

import java.rmi.UnexpectedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.DefaultParser
import printscript.Lexer
import printscript.domain.GrammarRule
import printscript.domain.SeqStep
import printscript.error.LexerError
import printscript.error.UnexpectedToken
import printscript.parse.step.StepEvaluator
import printscript.parse.step.StepOutcome
import printscript.support.Tokens
import printscript.support.atom
import printscript.support.capture
import printscript.support.evaluator
import printscript.support.expect
import printscript.support.grammar
import printscript.support.left
import printscript.support.parse
import printscript.support.ref
import printscript.support.repeat
import printscript.support.seq
import printscript.support.source
import printscript.syntax.Location
import printscript.syntax.SyntaxProgram
import printscript.token.LexerTokenSource
import printscript.util.Result

class ParserBranchCoverageTest {
    @Test
    fun `empty seq matches without children`() {
        val node = parse(grammar("s", "s" to seq()), Tokens.id("x"))

        assertEquals("s", node.name)
        assertEquals(0, node.children.size)
    }

    @Test
    fun `seq fails when a nested rule is missing after the first step`() {
        val g =
            grammar(
                "s",
                "s" to seq(expect("LET"), ref("n")),
                "n" to atom("NUMBER_LITERAL"),
            )
        val result = evaluator(g).evaluate("s", source(Tokens.let(), Tokens.id("x")))

        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `seq fails when a nested rule itself fails`() {
        val g =
            grammar(
                "s",
                "s" to seq(expect("LET"), ref("decl")),
                "decl" to seq(capture("ID"), expect("SEMICOLON")),
            )
        val result = evaluator(g).evaluate("s", source(Tokens.let(), Tokens.id("x"), Tokens.number("1")))

        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `hit without a node uses a null location by default`() {
        val hit = StepOutcome.Hit()

        assertEquals(null, hit.node)
        assertEquals(null, hit.location)
    }

    @Test
    fun `unknown seq step is rejected`() {
        assertThrows<IllegalStateException> {
            StepEvaluator().evaluate(FakeStep, parseContext())
        }
    }

    @Test
    fun `repeat of a seq that fails halfway is a failure`() {
        val g =
            grammar(
                "items",
                "items" to repeat("stmt"),
                "stmt" to seq(capture("ID"), expect("SEMICOLON")),
            )
        val result = evaluator(g).evaluate("items", source(Tokens.id("a"), Tokens.number("1")))

        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `repeat that matches without consuming tokens is rejected`() {
        val g =
            grammar(
                "items",
                "items" to repeat("empty"),
                "empty" to seq(),
            )

        assertThrows<IllegalStateException> {
            parse(g, Tokens.id("a"))
        }
    }

    @Test
    fun `left fails when the left operand itself fails`() {
        val g =
            grammar(
                "expr",
                "expr" to left("pair", "OPERATOR", "+"),
                "pair" to seq(expect("LET"), capture("ID")),
            )
        val result = evaluator(g).evaluate("expr", source(Tokens.let(), Tokens.number("1")))

        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `left does not match an operator without a value`() {
        val g =
            grammar(
                "expr",
                "expr" to left("num", "OPERATOR", "+"),
                "num" to atom("NUMBER_LITERAL"),
            )
        val tokens = source(Tokens.number("1"), Tokens.of("OPERATOR"), Tokens.number("2"))
        val result = evaluator(g).evaluate("expr", tokens)

        assertTrue(result is ParseResult.Matched)
        assertEquals("OPERATOR", tokens.peek().type)
    }

    @Test
    fun `evaluator rejects a rule with no handler`() {
        val rule =
            object : GrammarRule {
                override fun references(): List<String> = emptyList()
            }
        val g = printscript.domain.Grammar("s", mapOf("s" to rule))

        val error =
            assertThrows<IllegalStateException> {
                evaluator(g).evaluate("s", source(Tokens.eof()))
            }
        assertTrue(error.message!!.contains("No handler"))
    }

    @Test
    fun `token source throws when the lexer cannot read`() {
        assertThrows<UnexpectedException> {
            LexerTokenSource(FailingLexer()).peek()
        }
    }

    @Test
    fun `advancing at EOF stays on EOF`() {
        val tokens = source(Tokens.eof())

        assertEquals("EOF", tokens.advance().type)
        assertEquals("EOF", tokens.advance().type)
        assertTrue(tokens.isAtEnd())
    }

    @Test
    fun `parser reuses the token source for the same lexer`() {
        val lexer =
            printscript.support.MockLexer(
                listOf(
                    Tokens.number("1"),
                    Tokens.semicolon(),
                    Tokens.number("2"),
                    Tokens.semicolon(),
                ),
            )
        val parser = DefaultParser(simpleExprGrammar(), evaluator(simpleExprGrammar()))
        val builder = SyntaxProgram.builder()

        val first = parser.parseNextStatement(lexer)
        assertTrue(first is Result.Ok)
        builder.add((first as Result.Ok).value)
        val second = parser.parseNextStatement(lexer)
        assertTrue(second is Result.Ok)
        builder.add((second as Result.Ok).value)
        assertEquals(2, builder.build().statements.size)
    }

    private fun simpleExprGrammar() =
        grammar(
            "statement",
            "statement" to seq(ref("num"), expect("SEMICOLON")),
            "num" to atom("NUMBER_LITERAL"),
        )

    private fun parseContext(): ParseContext {
        val g = grammar("n", "n" to atom("NUMBER_LITERAL"))
        val tokens = source(Tokens.number("1"))
        return ParseContext(g, tokens) { name ->
            evaluator(g).evaluate(name, tokens)
        }
    }

    private object FakeStep : SeqStep

    private class FailingLexer : Lexer {
        override fun nextToken(): Result<printscript.domain.Token, LexerError> =
            Result.Err(UnexpectedToken(Location.empty()))

        override fun peek(offset: Int): Result<printscript.domain.Token, LexerError> =
            Result.Err(UnexpectedToken(Location.empty()))
    }
}
