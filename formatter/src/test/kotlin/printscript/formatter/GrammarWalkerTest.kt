package printscript.formatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.AtomRule
import printscript.domain.Grammar
import printscript.domain.RuleRefStep
import printscript.domain.SeqRule
import printscript.domain.SeqStep
import printscript.domain.TokenLexemes
import printscript.domain.TokenStep
import printscript.error.FormatError
import printscript.formatter.rules.TokenSpaceRule
import printscript.formatter.support.leaf
import printscript.formatter.support.program
import printscript.formatter.support.wrap
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.util.Result

class GrammarWalkerTest {
    @Test
    fun `emits two children that share a rule name in order`() {
        val grammar =
            Grammar(
                start = "pair",
                rules =
                    mapOf(
                        "pair" to SeqRule(listOf(RuleRefStep("item"), RuleRefStep("item"))),
                        "item" to AtomRule("ID"),
                    ),
            )
        val formatter = DefaultFormatterFactory.create(emptyList(), grammar, TokenLexemes(emptyMap()))
        val pair =
            SyntaxNode(
                name = "pair",
                children =
                    listOf(
                        wrap("item", leaf("ID", "a", 1)),
                        wrap("item", leaf("ID", "b", 2)),
                    ),
                location = Location.empty(),
            )

        assertEquals("ab", ok(formatter.format(program(pair))))
    }

    @Test
    fun `space after previous and before current collapse to one space`() {
        val grammar =
            Grammar(
                start = "decl",
                rules =
                    mapOf(
                        "decl" to
                            SeqRule(
                                listOf(
                                    TokenStep("LET", capture = false),
                                    TokenStep("ID", capture = true),
                                ),
                            ),
                    ),
            )
        val rules =
            listOf(
                TokenSpaceRule("LET", setOf(PointKind.AFTER_TOKEN), enabled = true),
                TokenSpaceRule("ID", setOf(PointKind.BEFORE_TOKEN), enabled = true),
            )
        val formatter =
            DefaultFormatterFactory.create(
                rules,
                grammar,
                TokenLexemes(mapOf("LET" to "let")),
            )
        val decl =
            SyntaxNode(
                name = "decl",
                children = listOf(leaf("ID", "x", 5)),
                location = Location.empty(),
            )

        assertEquals("let x", ok(formatter.format(program(decl))))
    }

    @Test
    fun `leftover children after a seq are unrecognized`() {
        val grammar =
            Grammar(
                start = "one",
                rules = mapOf("one" to SeqRule(listOf(TokenStep("ID", capture = true)))),
            )
        val formatter = DefaultFormatterFactory.create(emptyList(), grammar, TokenLexemes(emptyMap()))
        val node =
            SyntaxNode(
                name = "one",
                children = listOf(leaf("ID", "a", 1), leaf("ID", "b", 2)),
                location = Location.empty(),
            )

        assertTrue(formatter.format(program(node)) is Result.Err)
    }

    @Test
    fun `unknown seq step is unrecognized`() {
        val grammar =
            Grammar(
                start = "s",
                rules = mapOf("s" to SeqRule(listOf(FakeStep))),
            )
        val formatter = DefaultFormatterFactory.create(emptyList(), grammar, TokenLexemes(emptyMap()))
        val node = SyntaxNode(name = "s", location = Location.empty())

        assertTrue(formatter.format(program(node)) is Result.Err)
    }

    @Test
    fun `missing synthetic lexeme is unrecognized`() {
        val grammar =
            Grammar(
                start = "decl",
                rules =
                    mapOf(
                        "decl" to SeqRule(listOf(TokenStep("LET", capture = false), TokenStep("ID", capture = true))),
                    ),
            )
        val formatter = DefaultFormatterFactory.create(emptyList(), grammar, TokenLexemes(emptyMap()))
        val decl =
            SyntaxNode(
                name = "decl",
                children = listOf(leaf("ID", "x", 5)),
                location = Location.empty(),
            )

        assertTrue(formatter.format(program(decl)) is Result.Err)
    }

    @Test
    fun `empty program formats to empty string`() {
        val grammar =
            Grammar(
                start = "s",
                rules = mapOf("s" to AtomRule("ID")),
            )
        val formatter = DefaultFormatterFactory.create(emptyList(), grammar, TokenLexemes(emptyMap()))

        assertEquals("", ok(formatter.format(printscript.syntax.SyntaxProgram.empty())))
    }

    private object FakeStep : SeqStep

    private fun ok(result: Result<String, FormatError>): String {
        assertTrue(result is Result.Ok, "expected Ok but was $result")
        return (result as Result.Ok).value
    }
}
