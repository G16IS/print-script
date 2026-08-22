package lexer.tokenization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import printscript.RuleDrawResolver
import printscript.domain.ExactRule
import printscript.domain.LanguageConfig
import printscript.domain.RegexRule
import printscript.domain.TokenRule

class RuleDrawResolverTest {
    private val letRule = ExactRule(listOf("let"), "LET", false)
    private val printlnRule = ExactRule(listOf("println"), "CALL", true)
    private val plusRule = ExactRule(listOf("+"), "OPERATOR", false)
    private val assignRule = ExactRule(listOf("="), "ASSIGN", false)
    private val identifierRule =
        RegexRule(
            matcher = listOf("^[a-zA-Z_][a-zA-Z0-9_]*"),
            token = "IDENTIFIER",
            capture = true,
            partial = "^[a-zA-Z_]",
        )
    private val numberRule =
        RegexRule(
            matcher = listOf("^[0-9]+"),
            token = "NUMBER_LITERAL",
            capture = true,
            partial = "^[0-9]",
        )

    private fun languageConfig(
        order: List<String> = listOf("keywords", "operators", "literals", "identifiers"),
        config: Map<String, List<TokenRule>> =
            mapOf(
                "keywords" to listOf(letRule, printlnRule),
                "operators" to listOf(plusRule, assignRule),
                "literals" to listOf(numberRule),
                "identifiers" to listOf(identifierRule),
            ),
    ): LanguageConfig = LanguageConfig(order, config)

    private fun resolver(langConfig: LanguageConfig = languageConfig()) = RuleDrawResolver(langConfig)

    @Test
    fun `resolve returns the only matching rule`() {
        val result = resolver().resolve(listOf(letRule))

        assertEquals(letRule, result)
    }

    @Test
    fun `resolve returns the only matching regex rule`() {
        val result = resolver().resolve(listOf(identifierRule))

        assertEquals(identifierRule, result)
    }

    @Test
    fun `resolve prefers the category that appears last in order`() {
        val result = resolver().resolve(listOf(letRule, plusRule, identifierRule))

        assertEquals(identifierRule, result)
    }

    @Test
    fun `resolve prefers a later category over an earlier one`() {
        val result = resolver().resolve(listOf(letRule, plusRule))

        assertEquals(plusRule, result)
    }

    @Test
    fun `resolve still picks the highest priority when lower categories appear first`() {
        val result = resolver().resolve(listOf(identifierRule, numberRule, letRule))

        assertEquals(identifierRule, result)
    }

    @Test
    fun `resolve ignores extra rules from lower priority categories`() {
        val result = resolver().resolve(listOf(letRule, printlnRule, plusRule))

        assertEquals(plusRule, result)
    }

    @Test
    fun `resolve treats a category missing from order as lowest priority`() {
        val config = languageConfig(order = listOf("keywords", "operators"))

        val result = resolver(config).resolve(listOf(identifierRule, letRule))

        assertEquals(letRule, result)
    }

    @Test
    fun `resolve throws when matching rules is empty`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                resolver().resolve(emptyList())
            }

        assertEquals("No matching rules provided", exception.message)
    }

    @Test
    fun `resolve throws when two rules share the highest priority category`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                resolver().resolve(listOf(letRule, printlnRule))
            }

        assertEquals(
            "Multiple rules found with the same priority: ${listOf(letRule, printlnRule)}",
            exception.message,
        )
    }

    @Test
    fun `resolve throws when the highest category has more than one match`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                resolver().resolve(listOf(letRule, plusRule, assignRule))
            }

        assertEquals(
            "Multiple rules found with the same priority: ${listOf(plusRule, assignRule)}",
            exception.message,
        )
    }

    @Test
    fun `resolve throws when the rule is not present in the language config`() {
        val unknownRule = ExactRule(listOf("unknown"), "UNKNOWN", false)

        val exception =
            assertFailsWith<IllegalStateException> {
                resolver().resolve(listOf(unknownRule))
            }

        assertEquals("Rule $unknownRule not found in config", exception.message)
    }

    @Test
    fun `resolve throws when one of several rules is not present in the language config`() {
        val unknownRule = ExactRule(listOf("unknown"), "UNKNOWN", false)

        val exception =
            assertFailsWith<IllegalStateException> {
                resolver().resolve(listOf(letRule, unknownRule))
            }

        assertEquals("Rule $unknownRule not found in config", exception.message)
    }
}
