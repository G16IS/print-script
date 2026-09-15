package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import printscript.domain.ExactRule
import printscript.domain.RegexRule
import printscript.domain.TokenRule
import printscript.evaluator.MatchType
import printscript.evaluator.RuleEvaluator
import printscript.support.PrintScriptLanguage

class RuleEvaluatorTest {
    private val let = ExactRule(listOf("let"), "LET", false)
    private val type = ExactRule(listOf("string", "number"), "TYPE", true)
    private val identifier =
        RegexRule(
            matcher = listOf("^[a-zA-Z_][a-zA-Z0-9_]*"),
            token = "ID",
            capture = true,
            partial = "^[a-zA-Z_]",
        )
    private val number =
        RegexRule(
            matcher = listOf("^[0-9]+(\\.[0-9]+)?"),
            token = "NUMBER_LITERAL",
            capture = true,
            partial = PrintScriptLanguage.NUMBER_PARTIAL,
        )
    private val string =
        RegexRule(
            matcher = listOf("^\"[^\"]*\""),
            token = "STRING_LITERAL",
            capture = true,
            partial = PrintScriptLanguage.STRING_PARTIAL,
        )
    private val singleString =
        RegexRule(
            matcher = listOf("^'[^']*'"),
            token = "STRING_LITERAL",
            capture = true,
            partial = "^'[^']*$",
        )

    private val evaluator =
        RuleEvaluator(
            mapOf(
                "keywords" to listOf(let),
                "types" to listOf(type),
                "literals" to listOf(number, string, singleString),
                "identifiers" to listOf(identifier),
            ),
        )

    private fun match(
        text: String,
        rule: TokenRule,
    ): MatchType = evaluator.evaluate(text).single { it.tokenRule == rule }.matchType

    @Nested
    inner class Exact {
        @Test
        fun `full matcher is VALID`() {
            assertEquals(MatchType.VALID, match("let", let))
        }

        @Test
        fun `proper prefix is PARTIAL`() {
            assertEquals(MatchType.PARTIAL, match("l", let))
            assertEquals(MatchType.PARTIAL, match("le", let))
        }

        @Test
        fun `longer than every matcher is INVALID`() {
            assertEquals(MatchType.INVALID, match("letter", let))
            assertEquals(MatchType.INVALID, match("letx", let))
        }

        @Test
        fun `empty text is INVALID`() {
            assertEquals(MatchType.INVALID, match("", let))
        }

        @Test
        fun `any matcher in the list can match`() {
            assertEquals(MatchType.VALID, match("string", type))
            assertEquals(MatchType.VALID, match("number", type))
            assertEquals(MatchType.PARTIAL, match("str", type))
            assertEquals(MatchType.INVALID, match("numeral", type))
        }
    }

    @Nested
    inner class Regex {
        @Test
        fun `full matcher is VALID`() {
            assertEquals(MatchType.VALID, match("x", identifier))
            assertEquals(MatchType.VALID, match("lastName", identifier))
            assertEquals(MatchType.VALID, match("42", number))
            assertEquals(MatchType.VALID, match("1.5", number))
            assertEquals(MatchType.VALID, match("\"hello\"", string))
            assertEquals(MatchType.VALID, match("\"\"", string))
            assertEquals(MatchType.VALID, match("'hello'", singleString))
            assertEquals(MatchType.VALID, match("''", singleString))
        }

        @Test
        fun `partial-only prefixes stay PARTIAL`() {
            assertEquals(MatchType.PARTIAL, match("1.", number))
            assertEquals(MatchType.PARTIAL, match("\"hello", string))
            assertEquals(MatchType.PARTIAL, match("\"", string))
            assertEquals(MatchType.PARTIAL, match("'hello", singleString))
        }

        @Test
        fun `text that fails matcher and partial is INVALID`() {
            assertEquals(MatchType.INVALID, match("1.5.2", number))
            assertEquals(MatchType.INVALID, match("1e", number))
            assertEquals(MatchType.INVALID, match("\"hel\"lo", string))
            assertEquals(MatchType.INVALID, match("1", identifier))
        }

        @Test
        fun `empty text is INVALID`() {
            assertEquals(MatchType.INVALID, match("", identifier))
            assertEquals(MatchType.INVALID, match("", number))
        }
    }

    @Nested
    inner class AllRules {
        @Test
        fun `every rule is evaluated - no short circuit`() {
            val results = evaluator.evaluate("let")
            assertEquals(6, results.size)
            assertEquals(MatchType.VALID, results.single { it.tokenRule == let }.matchType)
            assertEquals(MatchType.VALID, results.single { it.tokenRule == identifier }.matchType)
            assertEquals(MatchType.INVALID, results.single { it.tokenRule == type }.matchType)
        }

        @Test
        fun `category is the map key`() {
            val result = evaluator.evaluate("let").single { it.tokenRule == let }
            assertEquals("keywords", result.category)
        }

        @Test
        fun `let is VALID for both the keyword and the identifier`() {
            assertEquals(MatchType.VALID, match("let", let))
            assertEquals(MatchType.VALID, match("let", identifier))
        }

        @Test
        fun `string is VALID for both TYPE and ID`() {
            assertEquals(MatchType.VALID, match("string", type))
            assertEquals(MatchType.VALID, match("string", identifier))
        }
    }

    @Nested
    inner class ProductionPartials {
        @Test
        fun `resource number partial does not accept a trailing dot`() {
            val rule =
                RegexRule(
                    matcher = listOf("^[0-9]+(\\.[0-9]+)?"),
                    token = "NUMBER_LITERAL",
                    capture = true,
                    partial = PrintScriptLanguage.PRODUCTION_NUMBER_PARTIAL,
                )
            val production = RuleEvaluator(mapOf("literals" to listOf(rule)))
            assertEquals(MatchType.VALID, production.evaluate("1").single().matchType)
            assertEquals(MatchType.INVALID, production.evaluate("1.").single().matchType)
            assertEquals(MatchType.VALID, production.evaluate("1.5").single().matchType)
        }

        @Test
        fun `resource string partial only matches a lone quote`() {
            val rule =
                RegexRule(
                    matcher = listOf("^\"[^\"]*\""),
                    token = "STRING_LITERAL",
                    capture = true,
                    partial = PrintScriptLanguage.PRODUCTION_STRING_PARTIAL,
                )
            val production = RuleEvaluator(mapOf("literals" to listOf(rule)))
            assertEquals(MatchType.PARTIAL, production.evaluate("\"").single().matchType)
            assertEquals(MatchType.INVALID, production.evaluate("\"h").single().matchType)
            assertEquals(MatchType.VALID, production.evaluate("\"hello\"").single().matchType)
        }

        @Test
        fun `test partials keep prefixes alive so TokenStream can finish the lexeme`() {
            assertEquals(MatchType.PARTIAL, match("1.", number))
            assertEquals(MatchType.VALID, match("1.5", number))
            assertEquals(MatchType.PARTIAL, match("\"h", string))
            assertEquals(MatchType.VALID, match("\"hello\"", string))
        }
    }

    @Nested
    inner class PrintScriptConfig {
        @Test
        fun `the shared language config evaluates the same way`() {
            val fromLanguage = RuleEvaluator(PrintScriptLanguage.config().config)
            val results = fromLanguage.evaluate("println")
            assertTrue(results.any { it.tokenRule.token == "CALL" && it.matchType == MatchType.VALID })
            assertTrue(results.any { it.tokenRule.token == "ID" && it.matchType == MatchType.VALID })
        }
    }
}
